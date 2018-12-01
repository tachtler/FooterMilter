/**
 * Copyright (c) 2018 Klaus Tachtler. All Rights Reserved.
 * Klaus Tachtler. <klaus@tachtler.net>
 * http://www.tachtler.net
 */
package net.tachtler.jmilter.FooterMilter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nullable;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.dom.BinaryBody;
import org.apache.james.mime4j.dom.Body;
import org.apache.james.mime4j.dom.Entity;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.MessageBuilder;
import org.apache.james.mime4j.dom.Multipart;
import org.apache.james.mime4j.dom.TextBody;
import org.apache.james.mime4j.dom.field.ContentTypeField;
import org.apache.james.mime4j.dom.field.FieldName;
import org.apache.james.mime4j.message.DefaultMessageBuilder;
import org.apache.james.mime4j.message.MessageImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.nightcode.milter.AbstractMilterHandler;
import org.nightcode.milter.MessageModificationService;
import org.nightcode.milter.MilterContext;
import org.nightcode.milter.MilterException;
import org.nightcode.milter.command.CommandProcessor;
import org.nightcode.milter.net.MilterPacket;
import org.nightcode.milter.util.Actions;
import org.nightcode.milter.util.ProtocolSteps;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.base64.Base64;

/*******************************************************************************
 * JMilter Handler for handling connections from an MTA to add a footer.
 * 
 * JMilter is an Open Source implementation of the Sendmail milter protocol, for
 * implementing milters in Java that can interface with the Sendmail or Postfix
 * MTA.
 * 
 * Java implementation of the Sendmail Milter protocol based on the project of
 * org.nightcode.jmilter from dmitry@nightcode.org.
 * 
 * @author Klaus Tachtler. <klaus@tachtler.net>
 * 
 *         Homepage : http://www.tachtler.net
 * 
 *         Licensed under the Apache License, Version 2.0 (the "License"); you
 *         may not use this file except in compliance with the License. You may
 *         obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *         implied. See the License for the specific language governing
 *         permissions and limitations under the License..
 * 
 *         Copyright (c) 2018 by Klaus Tachtler.
 ******************************************************************************/
public class FooterMilterHandler extends AbstractMilterHandler {

	private static Logger log = LogManager.getLogger();

	private static int timeout = 3;
	private static int ttl = 64;

	private String mailFrom = null;
	private Boolean footerAvailableResult = null;

	private StringBuffer parseContent = new StringBuffer();

	private MessageBuilder messageBuilder = new DefaultMessageBuilder();

	private Message message = null;
	private Body body = null;
	private StringBuffer bodyContent = new StringBuffer();
	private ContentTypeField contentTypeField = null;

	private FooterMilterInitBean argsBean = new FooterMilterInitBean(null, 0, null, null, null, null);

	private String entityTextBody = null;

	byte[] bodyModified = null;

	private StringBuffer addHeaderContent = new StringBuffer();

	/**
	 * @param milterActions
	 * @param milterProtocolSteps
	 */
	public FooterMilterHandler(Actions milterActions, ProtocolSteps milterProtocolSteps) {
		super(milterActions, milterProtocolSteps);
	}

	/**
	 * @param milterActions
	 * @param milterProtocolSteps
	 * @param messageModificationService
	 */
	public FooterMilterHandler(Actions milterActions, ProtocolSteps milterProtocolSteps,
			MessageModificationService messageModificationService) {
		super(milterActions, milterProtocolSteps, messageModificationService);
	}

	/**
	 * Extended to delivered argsBean (FooterMilterInitBean).
	 * 
	 * @param milterActions
	 * @param milterProtocolSteps
	 * @param argsBean
	 */
	public FooterMilterHandler(Actions milterActions, ProtocolSteps milterProtocolSteps,
			FooterMilterInitBean argsBean) {
		super(milterActions, milterProtocolSteps);
		this.argsBean = argsBean;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.nightcode.milter.AbstractMilterHandler#connect(org.nightcode.milter.
	 * MilterContext, java.lang.String, java.net.InetAddress)
	 */
	@Override
	public void connect(MilterContext context, String hostname, @Nullable InetAddress address) throws MilterException {

		log.debug("----------------------------------------: ");
		log.debug(
				"JMilter - ENTRY: connect                : MilterContext context, String hostname, @Nullable InetAddress address");
		log.debug("----------------------------------------: ");

		log.debug("*hostname                               : " + hostname);
		log.debug("*address.getCanonicalHostName()         : " + address.getCanonicalHostName());
		log.debug("*address.getHostAddress()               : " + address.getHostAddress());
		log.debug("*address.getHostName()                  : " + address.getHostName());

		byte[] addr = address.getAddress();
		short[] octet = new short[4];

		for (int i = 0; i <= addr.length - 1; i++) {
			if (addr[i] <= 127 && addr[i] >= -127 && addr[i] < 0) {
				octet[i] = (short) (addr[i] + 256);
			} else if (addr[i] <= 127 && addr[i] >= -127 && addr[i] > 0) {
				octet[i] = addr[i];
			} else {
				octet[i] = 0;
			}
		}

		log.debug("*address.getAddress()                   : " + "Octet: " + Arrays.toString(octet) + " / Byte: "
				+ Arrays.toString(addr));
		log.debug("*address.isAnyLocalAddress()            : " + address.isAnyLocalAddress());
		log.debug("*address.isLinkLocalAddress()           : " + address.isLinkLocalAddress());
		log.debug("*address.isLoopbackAddress()            : " + address.isLoopbackAddress());
		log.debug("*address.isMCGlobal()                   : " + address.isMCGlobal());
		log.debug("*address.isMCLinkLocal()                : " + address.isMCLinkLocal());
		log.debug("*address.isMCNodeLocal()                : " + address.isMCNodeLocal());
		log.debug("*address.isMCOrgLocal()                 : " + address.isMCOrgLocal());
		log.debug("*address.isMCSiteLocal()                : " + address.isMCSiteLocal());
		log.debug("*address.isMulticastAddress()           : " + address.isMulticastAddress());

		try {
			log.debug("*address.isReachable(timeout)           : " + address.isReachable(timeout));
		} catch (IOException eIsReachableTimeout) {
			log.error(
					"***** Program stop, because FooterMilter could not be initialized! ***** (For more details, see error messages and caused by below).");
			log.error("IOException                             : " + eIsReachableTimeout);
			log.error(ExceptionUtils.getStackTrace(eIsReachableTimeout));
		}

		NetworkInterface netif = null;
		try {
			netif = NetworkInterface.getByInetAddress(address);

			if (netif != null) {

				log.debug("*netif.getDisplayName()                 : " + netif.getDisplayName());
				log.debug("*netif.getIndex()                       : " + netif.getIndex());
				log.debug("*netif.getMTU()                         : " + netif.getMTU());
				log.debug("*netif.getName()                        : " + netif.getName());

				byte[] hwAddr = netif.getHardwareAddress();
				String hwAddrString = null;
				StringBuilder stringBuilder = new StringBuilder();

				if (hwAddr != null && hwAddr.length > 0) {
					for (byte b : hwAddr) {
						stringBuilder.append(String.format("%02x:", b));
					}
					hwAddrString = stringBuilder.deleteCharAt(stringBuilder.length() - 1).toString();
				}

				log.debug("*netif.getHardwareAddress()             : " + hwAddrString);

				Enumeration<InetAddress> inetAddresses = netif.getInetAddresses();
				for (InetAddress inetAddress : Collections.list(inetAddresses)) {
					log.debug("*netif.getInetAddresses()               : " + inetAddress);
				}

				log.debug("*netif.getInterfaceAddresses()          : " + netif.getInterfaceAddresses());
				log.debug("*netif.getParent()                      : " + netif.getParent());

				Enumeration<NetworkInterface> networkInterfaces = netif.getSubInterfaces();
				for (NetworkInterface networkInterface : Collections.list(networkInterfaces)) {
					log.debug("*netif.getSubInterfaces()               : " + networkInterface);
				}

				log.debug("*netif.isLoopback()                     : " + netif.isLoopback());
				log.debug("*netif.isPointToPoint()                 : " + netif.isPointToPoint());
				log.debug("*netif.isUp()                           : " + netif.isUp());
				log.debug("*netif.isVirtual()                      : " + netif.isVirtual());
				log.debug("*netif.supportsMulticast()              : " + netif.supportsMulticast());
			}

		} catch (SocketException eSocketException) {
			log.error(
					"***** Program stop, because FooterMilter could not be initialized! ***** (For more details, see error messages and caused by below).");
			log.error("SocketException                         : " + eSocketException);
			log.error(ExceptionUtils.getStackTrace(eSocketException));
		}

		try {
			log.debug("*address.isReachable(netif, ttl, time...: " + address.isReachable(netif, ttl, timeout));
		} catch (IOException eIOException) {
			log.error(
					"***** Program stop, because FooterMilter could not be initialized! ***** (For more details, see error messages and caused by below).");
			log.error("IOException                             : " + eIOException);
			log.error(ExceptionUtils.getStackTrace(eIOException));
		}
		log.debug("*address.isSiteLocalAddress()           : " + address.isSiteLocalAddress());

		logContext(context);
		logContext(context, CommandProcessor.SMFIC_CONNECT);

		log.debug("----------------------------------------: ");
		log.debug(
				"JMilter - LEAVE: connect                : MilterContext context, String hostname, @Nullable InetAddress address");
		log.debug("----------------------------------------: ");

		/*
		 * Change the SMFIS action possible values are:
		 *
		 * SMFIS_CONTINUE, SMFIS_REJECT, SMFIS_DISCARD, SMFIS_ACCEPT, SMFIS_TEMPFAIL,
		 * SMFIS_SKIP
		 *
		 * context.sendPacket(MilterPacketUtil.SMFIS_CONTINUE);
		 */

		super.connect(context, hostname, address);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.nightcode.milter.AbstractMilterHandler#helo(org.nightcode.milter.
	 * MilterContext, java.lang.String)
	 */
	@Override
	public void helo(MilterContext context, String helohost) throws MilterException {

		log.debug("----------------------------------------: ");
		log.debug("JMilter - ENTRY: helo                   : MilterContext context, String helohost");
		log.debug("----------------------------------------: ");

		log.debug("*helohost                               : " + helohost);

		logContext(context);
		logContext(context, CommandProcessor.SMFIC_CONNECT);
		logContext(context, CommandProcessor.SMFIC_HELO);

		log.debug("----------------------------------------: ");
		log.debug("JMilter - LEAVE: helo                   : MilterContext context, String helohost");
		log.debug("----------------------------------------: ");

		super.helo(context, helohost);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.nightcode.milter.AbstractMilterHandler#envfrom(org.nightcode.milter.
	 * MilterContext, java.util.List)
	 */
	@Override
	public void envfrom(MilterContext context, List<String> from) throws MilterException {

		/*
		 * Detect if the from email address is available inside the mapText or mapHtml.
		 * The variable result will be true or false and the variable mailFrom will be
		 * the mail_from address, "@domain.tld" or null.
		 */
		isFooterAvailable(context);

		log.debug("*isFooterAvailable (envfrom)            : " + footerAvailableResult);

		log.debug("----------------------------------------: ");
		log.debug("JMilter - ENTRY: envfrom                : MilterContext context, List<String> from");
		log.debug("----------------------------------------: ");

		for (int i = 0; i <= from.size() - 1; i++) {
			log.debug("*from.get(i)                            : " + "[" + i + "] " + from.get(i));
		}

		logContext(context);
		logContext(context, CommandProcessor.SMFIC_CONNECT);
		logContext(context, CommandProcessor.SMFIC_HELO);
		logContext(context, CommandProcessor.SMFIC_MAIL);

		log.debug("----------------------------------------: ");
		log.debug("JMilter - LEAVE: envfrom                : MilterContext context, List<String> from");
		log.debug("----------------------------------------: ");

		super.envfrom(context, from);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.nightcode.milter.AbstractMilterHandler#envrcpt(org.nightcode.milter.
	 * MilterContext, java.util.List)
	 */
	@Override
	public void envrcpt(MilterContext context, List<String> recipients) throws MilterException {

		log.debug("----------------------------------------: ");
		log.debug("JMilter - ENTRY: envrcpt                : MilterContext context, List<String> recipients");
		log.debug("----------------------------------------: ");

		for (int i = 0; i <= recipients.size() - 1; i++) {
			log.debug("*recipients.get(i)                      : " + "[" + i + "] " + recipients.get(i));
		}

		logContext(context);
		logContext(context, CommandProcessor.SMFIC_CONNECT);
		logContext(context, CommandProcessor.SMFIC_HELO);
		logContext(context, CommandProcessor.SMFIC_MAIL);
		logContext(context, CommandProcessor.SMFIC_RCPT);

		log.debug("----------------------------------------: ");
		log.debug("JMilter - LEAVE: envrcpt                : MilterContext context, List<String> recipients");
		log.debug("----------------------------------------: ");

		super.envrcpt(context, recipients);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.nightcode.milter.AbstractMilterHandler#data(org.nightcode.milter.
	 * MilterContext, byte[])
	 */
	@Override
	public void data(MilterContext context, byte[] payload) throws MilterException {

		log.debug("----------------------------------------: ");
		log.debug("JMilter - ENTRY: data                   : MilterContext context, byte[] payload");
		log.debug("----------------------------------------: ");

		byte[] dataPayload = payload;
		String dataPayloadString = null;
		StringBuilder stringBuilder = new StringBuilder();

		if (dataPayload != null && dataPayload.length > 0) {
			for (byte b : dataPayload) {
				stringBuilder.append(String.format("%02x:", b));
			}
			dataPayloadString = stringBuilder.deleteCharAt(stringBuilder.length() - 1).toString();
		}

		log.debug("*payload                                : " + dataPayloadString);

		logContext(context);
		logContext(context, CommandProcessor.SMFIC_CONNECT);
		logContext(context, CommandProcessor.SMFIC_HELO);
		logContext(context, CommandProcessor.SMFIC_MAIL);
		logContext(context, CommandProcessor.SMFIC_RCPT);
		logContext(context, CommandProcessor.SMFIC_DATA);

		log.debug("----------------------------------------: ");
		log.debug("JMilter - LEAVE: data                   : MilterContext context, byte[] payload");
		log.debug("----------------------------------------: ");

		super.data(context, payload);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.nightcode.milter.AbstractMilterHandler#header(org.nightcode.milter.
	 * MilterContext, java.lang.String, java.lang.String)
	 */
	@Override
	public void header(MilterContext context, String headerName, String headerValue) throws MilterException {

		log.debug("*isFooterAvailable (header)             : " + footerAvailableResult);

		/*
		 * Check if the from email address is available inside the mapText or mapHtml.
		 * If true, continue adding a foot, else do nothing.
		 */
		if (footerAvailableResult) {

			/*
			 * Concatenate every headerName and headerValue to a formated single line.
			 */
			parseContent.append(headerName);
			parseContent.append(": ");
			parseContent.append(headerValue);
			parseContent.append(System.lineSeparator());

			log.debug("*parseContent.toString()                : " + parseContent.toString());
		}

		log.debug("----------------------------------------: ");
		log.debug(
				"JMilter - ENTRY: header                 : MilterContext context, String headerName, String headerValue");
		log.debug("----------------------------------------: ");

		log.debug("*headerName: headerValue                : " + headerName + ": " + headerValue);

		logContext(context);
		logContext(context, CommandProcessor.SMFIC_CONNECT);
		logContext(context, CommandProcessor.SMFIC_HELO);
		logContext(context, CommandProcessor.SMFIC_MAIL);
		logContext(context, CommandProcessor.SMFIC_RCPT);
		logContext(context, CommandProcessor.SMFIC_DATA);
		logContext(context, CommandProcessor.SMFIC_HEADER);

		log.debug("----------------------------------------: ");
		log.debug(
				"JMilter - LEAVE: header                 : MilterContext context, String headerName, String headerValue");
		log.debug("----------------------------------------: ");

		super.header(context, headerName, headerValue);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.nightcode.milter.AbstractMilterHandler#eoh(org.nightcode.milter.
	 * MilterContext)
	 */
	@Override
	public void eoh(MilterContext context) throws MilterException {

		log.debug("----------------------------------------: ");
		log.debug("JMilter - ENTRY: eoh                    : MilterContext context");
		log.debug("----------------------------------------: ");

		logContext(context);
		logContext(context, CommandProcessor.SMFIC_CONNECT);
		logContext(context, CommandProcessor.SMFIC_HELO);
		logContext(context, CommandProcessor.SMFIC_MAIL);
		logContext(context, CommandProcessor.SMFIC_RCPT);
		logContext(context, CommandProcessor.SMFIC_DATA);
		logContext(context, CommandProcessor.SMFIC_HEADER);
		logContext(context, CommandProcessor.SMFIC_EOH);

		log.debug("----------------------------------------: ");
		log.debug("JMilter - LEAVE: eoh                    : MilterContext context");
		log.debug("----------------------------------------: ");

		super.eoh(context);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.nightcode.milter.AbstractMilterHandler#body(org.nightcode.milter.
	 * MilterContext, java.lang.String)
	 */
	@Override
	public void body(MilterContext context, String bodyChunk) throws MilterException {

		log.debug("*isFooterAvailable (body)               : " + footerAvailableResult);

		/*
		 * Check if the from email address is available inside the mapText or mapHtml.
		 * If true, continue adding a foot, else do nothing.
		 */
		if (footerAvailableResult) {

			/*
			 * Add the bodyChunk to the formated header lines to parseContent.
			 */
			parseContent.append(System.lineSeparator());
			parseContent.append(bodyChunk);

			log.debug("*parseContent <- (Start at next line) ->: " + System.lineSeparator() + parseContent.toString());

		}

		log.debug("----------------------------------------: ");
		log.debug("JMilter - ENTRY: body                   : MilterContext context, String bodyChunk");
		log.debug("----------------------------------------: ");

		log.debug("*bodyChunk <-- (Start at next line) --> : " + System.lineSeparator() + bodyChunk);

		logContext(context);
		logContext(context, CommandProcessor.SMFIC_CONNECT);
		logContext(context, CommandProcessor.SMFIC_HELO);
		logContext(context, CommandProcessor.SMFIC_MAIL);
		logContext(context, CommandProcessor.SMFIC_RCPT);
		logContext(context, CommandProcessor.SMFIC_DATA);
		logContext(context, CommandProcessor.SMFIC_HEADER);
		logContext(context, CommandProcessor.SMFIC_EOH);
		logContext(context, CommandProcessor.SMFIC_BODY);

		log.debug("----------------------------------------: ");
		log.debug("JMilter - LEAVE: body                   : MilterContext context, String bodyChunk");
		log.debug("----------------------------------------: ");

		super.body(context, bodyChunk);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.nightcode.milter.AbstractMilterHandler#eom(org.nightcode.milter.
	 * MilterContext, java.lang.String)
	 */
	@Override
	public void eom(MilterContext context, @Nullable String bodyChunk) throws MilterException {

		log.debug("*isFooterAvailable (eom)                : " + footerAvailableResult);

		/*
		 * Check if the from email address is available inside the mapText or mapHtml.
		 * If true, continue adding a foot, else do nothing.
		 */
		if (footerAvailableResult) {

			/*
			 * Generate the modified Body with the necessary footer added.
			 */
			generateModifiedBody();

			log.debug("*bodyContent.toString()                 : " + bodyContent.toString());

			/*
			 * Copy the modified String into the bodyModified byte array.
			 */
			bodyModified = bodyContent.toString().getBytes(StandardCharsets.UTF_8);

			/*
			 * Replace the original body with the modified bodyModified byte array.
			 */
			messageModificationService.replaceBody(context, bodyModified);

			log.debug("messageModificationService.replaceBody  : " + bodyModified.toString());

			/*
			 * Add the header tag for mail body modifying (using footer) - CR/LF
			 * {daemon_name}.
			 */
			addHeaderContent.append("Mail body modified (using footer)");
			addHeaderContent.append(System.lineSeparator());
			addHeaderContent.append("by ");
			addHeaderContent.append(context.getMacros(CommandProcessor.SMFIC_CONNECT).get("{daemon_name}").toString());
			addHeaderContent.append(System.lineSeparator());
			addHeaderContent.append("for <");
			addHeaderContent.append(mailFrom);
			addHeaderContent.append(">");

			messageModificationService.addHeader(context, "X-FooterMilter-Modified", addHeaderContent.toString());

			log.debug("messageModificationService.addHeader    : " + "X-FooterMilter-Modified: "
					+ addHeaderContent.toString());

		}

		log.debug("----------------------------------------: ");
		log.debug("JMilter - ENTRY: eom                    : MilterContext context, @Nullable String bodyChunk");
		log.debug("----------------------------------------: ");

		log.debug("*bodyChunk <-- (Start at next line) --> : " + System.lineSeparator() + bodyChunk);

		logContext(context);
		logContext(context, CommandProcessor.SMFIC_CONNECT);
		logContext(context, CommandProcessor.SMFIC_HELO);
		logContext(context, CommandProcessor.SMFIC_MAIL);
		logContext(context, CommandProcessor.SMFIC_RCPT);
		logContext(context, CommandProcessor.SMFIC_DATA);
		logContext(context, CommandProcessor.SMFIC_HEADER);
		logContext(context, CommandProcessor.SMFIC_EOH);
		logContext(context, CommandProcessor.SMFIC_BODY);
		logContext(context, CommandProcessor.SMFIC_BODYEOB);

		log.debug("----------------------------------------: ");
		log.debug("JMilter - LEAVE: eom                    : MilterContext context, @Nullable String bodyChunk");
		log.debug("----------------------------------------: ");

		super.eom(context, bodyChunk);

		/*
		 * Initialize global variables after every email delivery.
		 */
		initGlobalVariables();

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.nightcode.milter.AbstractMilterHandler#abort(org.nightcode.milter.
	 * MilterContext, org.nightcode.milter.net.MilterPacket)
	 */
	@Override
	public void abort(MilterContext context, MilterPacket packet) throws MilterException {

		log.debug("----------------------------------------: ");
		log.debug("JMilter - ENTRY: abort                  : MilterContext context, MilterPacket packet");
		log.debug("----------------------------------------: ");

		log.debug("*packet                                 : " + packet);

		logContext(context);
		logContext(context, CommandProcessor.SMFIC_CONNECT);
		logContext(context, CommandProcessor.SMFIC_HELO);
		logContext(context, CommandProcessor.SMFIC_MAIL);
		logContext(context, CommandProcessor.SMFIC_RCPT);
		logContext(context, CommandProcessor.SMFIC_DATA);
		logContext(context, CommandProcessor.SMFIC_HEADER);
		logContext(context, CommandProcessor.SMFIC_EOH);
		logContext(context, CommandProcessor.SMFIC_BODY);
		logContext(context, CommandProcessor.SMFIC_BODYEOB);
		logContext(context, CommandProcessor.SMFIC_ABORT);

		log.debug("----------------------------------------: ");
		log.debug("JMilter - LEAVE: abort                  : MilterContext context, MilterPacket packet");
		log.debug("----------------------------------------: ");

		/*
		 * !IMPORTANT
		 * 
		 * Disabled, because it will break the delivery after the email will come BACK
		 * from smtp_proxy_filter or content_filter.
		 * 
		 * Many thanks to dmitry@nightcode.org for the support.
		 */
		// super.abort(context, packet);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.nightcode.milter.AbstractMilterHandler#negotiate(org.nightcode.milter.
	 * MilterContext, int, org.nightcode.milter.util.Actions,
	 * org.nightcode.milter.util.ProtocolSteps)
	 */
	@Override
	public void negotiate(MilterContext context, int mtaProtocolVersion, Actions mtaActions,
			ProtocolSteps mtaProtocolSteps) throws MilterException {

		log.debug("----------------------------------------: ");
		log.debug(
				"JMilter - ENTRY: negotiate              : MilterContext context, int mtaProtocolVersion, Actions mtaActions, ProtocolSteps mtaProtocolSteps");
		log.debug("----------------------------------------: ");

		log.debug("*mtaProtocolVersion                     : " + mtaProtocolVersion);
		log.debug("*mtaActions                             : " + mtaActions);
		log.debug("*mtaProtocolSteps                       : " + mtaProtocolSteps);

		logContext(context);
		logContext(context, CommandProcessor.SMFIC_CONNECT);
		logContext(context, CommandProcessor.SMFIC_HELO);
		logContext(context, CommandProcessor.SMFIC_MAIL);
		logContext(context, CommandProcessor.SMFIC_RCPT);
		logContext(context, CommandProcessor.SMFIC_DATA);
		logContext(context, CommandProcessor.SMFIC_HEADER);
		logContext(context, CommandProcessor.SMFIC_EOH);
		logContext(context, CommandProcessor.SMFIC_BODY);
		logContext(context, CommandProcessor.SMFIC_BODYEOB);
		logContext(context, CommandProcessor.SMFIC_ABORT);
		logContext(context, CommandProcessor.SMFIC_OPTNEG);

		log.debug("----------------------------------------: ");
		log.debug(
				"JMilter - LEAVE: negotiate              : MilterContext context, int mtaProtocolVersion, Actions mtaActions, ProtocolSteps mtaProtocolSteps");
		log.debug("----------------------------------------: ");

		super.negotiate(context, mtaProtocolVersion, mtaActions, mtaProtocolSteps);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.nightcode.milter.AbstractMilterHandler#unknown(org.nightcode.milter.
	 * MilterContext, byte[])
	 */
	@Override
	public void unknown(MilterContext context, byte[] payload) throws MilterException {

		log.debug("----------------------------------------: ");
		log.debug("JMilter - ENTRY: unknown                : MilterContext context, byte[] payload");
		log.debug("----------------------------------------: ");

		byte[] dataPayload = payload;
		String dataPayloadString = null;
		StringBuilder stringBuilder = new StringBuilder();

		if (dataPayload != null && dataPayload.length > 0) {
			for (byte b : dataPayload) {
				stringBuilder.append(String.format("%02x:", b));
			}
			dataPayloadString = stringBuilder.deleteCharAt(stringBuilder.length() - 1).toString();
		}

		log.debug("*payload                                : " + dataPayloadString);

		logContext(context);
		logContext(context, CommandProcessor.SMFIC_CONNECT);
		logContext(context, CommandProcessor.SMFIC_HELO);
		logContext(context, CommandProcessor.SMFIC_MAIL);
		logContext(context, CommandProcessor.SMFIC_RCPT);
		logContext(context, CommandProcessor.SMFIC_DATA);
		logContext(context, CommandProcessor.SMFIC_HEADER);
		logContext(context, CommandProcessor.SMFIC_EOH);
		logContext(context, CommandProcessor.SMFIC_BODY);
		logContext(context, CommandProcessor.SMFIC_BODYEOB);
		logContext(context, CommandProcessor.SMFIC_ABORT);
		logContext(context, CommandProcessor.SMFIC_OPTNEG);
		logContext(context, CommandProcessor.SMFIC_UNKNOWN);

		log.debug("----------------------------------------: ");
		log.debug("JMilter - LEAVE: unknown                : MilterContext context, byte[] payload");
		log.debug("----------------------------------------: ");

		super.unknown(context, payload);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.nightcode.milter.MilterHandler#close(org.nightcode.milter.MilterContext)
	 */
	@Override
	public void close(MilterContext arg0) {

		log.debug("----------------------------------------: ");
		log.debug("JMilter - ENTRY: close                  : MilterContext arg0");
		log.debug("----------------------------------------: ");

		log.debug("----------------------------------------: ");
		log.debug("JMilter - LEAVE: close                  : MilterContext arg0");
		log.debug("----------------------------------------: ");
	}

	/**
	 * Check if the from email address is available inside the mapText or mapHtml.
	 * If true, continue adding a footer by setting the mailFrom variable, else
	 * check if @domain.tld was found inside the mapText or mapHtml. If true,
	 * continue adding a footer by setting the mailFrom variable to @domain.tld.
	 * Else, do nothing, by setting the result variable to false.
	 * 
	 * @param context
	 * @return Boolean
	 */
	private void isFooterAvailable(MilterContext context) {

		if (argsBean.getMapText()
				.containsKey(context.getMacros(CommandProcessor.SMFIC_MAIL).get("{mail_addr}").toString())
				|| argsBean.getMapHtml()
						.containsKey(context.getMacros(CommandProcessor.SMFIC_MAIL).get("{mail_addr}").toString())) {

			mailFrom = context.getMacros(CommandProcessor.SMFIC_MAIL).get("{mail_addr}").toString();

			footerAvailableResult = true;
		} else {
			if (argsBean.getMapText()
					.containsKey(context.getMacros(CommandProcessor.SMFIC_MAIL).get("{mail_addr}").toString().substring(
							context.getMacros(CommandProcessor.SMFIC_MAIL).get("{mail_addr}").toString().indexOf("@")))
					|| argsBean.getMapHtml()
							.containsKey(context.getMacros(CommandProcessor.SMFIC_MAIL).get("{mail_addr}").toString()
									.substring(context.getMacros(CommandProcessor.SMFIC_MAIL).get("{mail_addr}")
											.toString().indexOf("@")))) {

				context.getMacros(CommandProcessor.SMFIC_MAIL).put("{mail_addr}",
						context.getMacros(CommandProcessor.SMFIC_MAIL).get("{mail_addr}").toString().substring(context
								.getMacros(CommandProcessor.SMFIC_MAIL).get("{mail_addr}").toString().indexOf("@")));

				mailFrom = context.getMacros(CommandProcessor.SMFIC_MAIL).get("{mail_addr}").toString().substring(
						context.getMacros(CommandProcessor.SMFIC_MAIL).get("{mail_addr}").toString().indexOf("@"));

				footerAvailableResult = true;
			} else {

				footerAvailableResult = false;

				/*
				 * Iterate over the mapText, and if a from address entry will be a part of the
				 * mail_addr, then take that match as mailFrom.
				 */
				Iterator<Entry<String, String>> iteratorMapText = argsBean.getMapText().entrySet().iterator();
				while (iteratorMapText.hasNext()) {
					Map.Entry<String, String> pair = (Map.Entry<String, String>) iteratorMapText.next();
					if (context.getMacros(CommandProcessor.SMFIC_MAIL).get("{mail_addr}").toString()
							.substring(context.getMacros(CommandProcessor.SMFIC_MAIL).get("{mail_addr}").toString()
									.indexOf("@") + 1)
							.contains(pair.getKey().substring(pair.getKey().indexOf("@") + 1))) {
						mailFrom = pair.getKey();
						footerAvailableResult = true;
						break;
					}

				}

				/*
				 * Iterate over the mapHtml, and if a from address entry will be a part of the
				 * mail_addr, then take that match as mailFrom.
				 */
				Iterator<Entry<String, String>> iteratorMapHtml = argsBean.getMapHtml().entrySet().iterator();
				while (iteratorMapHtml.hasNext()) {
					Map.Entry<String, String> pair = (Map.Entry<String, String>) iteratorMapHtml.next();
					if (context.getMacros(CommandProcessor.SMFIC_MAIL).get("{mail_addr}").toString()
							.substring(context.getMacros(CommandProcessor.SMFIC_MAIL).get("{mail_addr}").toString()
									.indexOf("@") + 1)
							.contains(pair.getKey().substring(pair.getKey().indexOf("@") + 1))) {
						mailFrom = pair.getKey();
						footerAvailableResult = true;
						break;
					}

				}

			}

		}

		log.debug("*mailFrom                               : " + mailFrom);

	}

	/**
	 * Convert from Entity part/message.getBody() to String.
	 * 
	 * @param entity
	 * @return String
	 */
	private String getTextBody(Entity entity) {
		TextBody textBody = (TextBody) entity.getBody();
		StringBuffer stringBuffer = new StringBuffer();
		try {
			Reader reader = textBody.getReader();
			int count;
			while ((count = reader.read()) != -1) {
				stringBuffer.append((char) count);
			}
		} catch (IOException eIOException) {
			log.error(
					"***** Program stop, because FooterMilter detects a runtime error! ***** (For more details, see error messages and caused by below).");
			log.error("IOException                             : " + eIOException);
			log.error(ExceptionUtils.getStackTrace(eIOException));
		}

		log.debug("getTextBody  <- (Start at next line) -> : " + System.lineSeparator() + stringBuffer.toString());

		return stringBuffer.toString();
	}

	/**
	 * Convert from Entity part/message.getBody() to String.
	 * 
	 * @param part
	 * @return String
	 */
	private String getBinaryBody(Entity entity) {
		BinaryBody binaryBody = (BinaryBody) entity.getBody();
		String binaryBodyString = null;
		try {
			InputStream inputStream = binaryBody.getInputStream();
			byte[] bytes = IOUtils.toByteArray(inputStream);

			if (entity.getContentTransferEncoding().equalsIgnoreCase("base64")) {
				ByteBuf byteBuf = Unpooled.buffer(bytes.length);
				byteBuf.writeBytes(bytes);
				binaryBodyString = Base64.encode(byteBuf, true).toString(StandardCharsets.UTF_8);
			} else {
				binaryBodyString = new String(bytes);
			}

		} catch (IOException eIOException) {
			log.error(
					"***** Program stop, because FooterMilter detects a runtime error! ***** (For more details, see error messages and caused by below).");
			log.error("IOException                             : " + eIOException);
			log.error(ExceptionUtils.getStackTrace(eIOException));
		}

		log.debug("getBinaryBody <- (Start at next line) -> : " + System.lineSeparator() + binaryBodyString);

		return binaryBodyString;
	}

	/**
	 * Add a text/plain footer to the bodyContent, BUT only if it's NOT
	 * "Content-Disposition: attachment;".
	 * 
	 * @param part
	 */
	private void addTextWithFooter(Entity entity) {
		bodyContent.append(getTextBody(entity));

		log.debug("*part.getDispositionType()              : " + entity.getDispositionType());

		if (null != entity.getDispositionType()) {
			if (!entity.getDispositionType().equalsIgnoreCase("attachment")) {
				bodyContent.append(argsBean.getMapText().get(mailFrom));
				bodyContent.append(System.lineSeparator());
			}
		} else {
			bodyContent.append(argsBean.getMapText().get(mailFrom));
			bodyContent.append(System.lineSeparator());
		}

		log.debug("Content-Type: text/plain                : ");
	}

	/**
	 * Add a text/html footer to the body content, if the is NOT
	 * "Content-Disposition: attachment;".
	 * 
	 * @param part
	 */
	private void addHtmlWithFooter(Entity entity) {

		/*
		 * Check if a well formed HTML content will be found. If it's true, customize
		 * the well formed HTML content. If it's false, add the HTML content at the end
		 * of the multipart part.
		 */
		entityTextBody = getTextBody(entity);

		if (entityTextBody.indexOf("</body>") != -1) {
			String[] splitString = entityTextBody.split("</body>");
			bodyContent.append(splitString[0].toString());

			if (null != entity.getDispositionType()) {
				if (!entity.getDispositionType().equalsIgnoreCase("attachment")) {
					bodyContent.append(argsBean.getMapHtml().get(mailFrom));
				}
			} else {
				bodyContent.append(argsBean.getMapHtml().get(mailFrom));
			}

			bodyContent.append("</body>");
			bodyContent.append(splitString[1].toString());

			log.debug("Content-Type: text/html formatted       : ");
		} else {
			bodyContent.append(entityTextBody);

			if (null != entity.getDispositionType()) {
				if (!entity.getDispositionType().equalsIgnoreCase("attachment")) {
					bodyContent.append(argsBean.getMapHtml().get(mailFrom));
				}
			} else {
				bodyContent.append(argsBean.getMapHtml().get(mailFrom));
			}

			log.debug("Content-Type: text/html unformatted     : ");
		}

		log.debug("Content-Type: text/html                 : ");

	}

	/**
	 * Add a binary content to the body content without any modification.
	 * 
	 * @param part
	 */
	private void addBinaryContent(Entity entity) {
		bodyContent.append(getBinaryBody(entity));
		bodyContent.append(System.lineSeparator());
	}

	/**
	 * Generate the modified Body from multipart ord single message with the
	 * different part typs like text/plain, text/html and binary parts if detected.
	 */
	private void generateModifiedBody() {

		/*
		 * Generate the message parsing the parseContent with the messageBuilder.
		 */
		try {
			message = messageBuilder
					.parseMessage(new ByteArrayInputStream(parseContent.toString().getBytes(StandardCharsets.UTF_8)));
		} catch (MimeException eMimeException) {
			log.error(
					"***** Program stop, because FooterMilter detects a runtime error! ***** (For more details, see error messages and caused by below).");
			log.error("MimeException                           : " + eMimeException);
			log.error(ExceptionUtils.getStackTrace(eMimeException));
		} catch (IOException eIOException) {
			log.error(
					"***** Program stop, because FooterMilter detects a runtime error! ***** (For more details, see error messages and caused by below).");
			log.error("IOException                             : " + eIOException);
			log.error(ExceptionUtils.getStackTrace(eIOException));
		}

		/*
		 * Start creating the new body.
		 */
		createBody(message);

	}

	/**
	 * Creates the body parts from a given MIME entity (either a Message or a
	 * BodyPart).
	 * 
	 */
	private void createBody(Entity entity) {

		body = entity.getBody();

		if (body instanceof Multipart) {
			createBody((Multipart) body);
		} else if (body instanceof MessageImpl) {
			createBody((MessageImpl) body);
		} else if (body instanceof TextBody) {

			/*
			 * Determine the multipart entity content type and insert dependently the
			 * content type of the footer.
			 */
			if (entity.getMimeType().equalsIgnoreCase("text/plain")) {
				addTextWithFooter(entity);
			} else if (entity.getMimeType().equalsIgnoreCase("text/html")) {
				addHtmlWithFooter(entity);
			}

		} else if (body instanceof BinaryBody) {
			addBinaryContent(entity);
		}

	}

	/**
	 * Create a body part form a given multipart body. Add the "preamble", all body
	 * parts and the "epilogue" to the bodyModified.
	 * 
	 * @param multipart
	 */
	private void createBody(Multipart multipart) {

		/*
		 * If available, add "preamble" before the multipart messages.
		 */
		if (null != multipart.getPreamble()) {
			bodyContent.append(multipart.getPreamble());
			bodyContent.append(System.lineSeparator());
		}

		/*
		 * Iterate over the parts of the multipart message.
		 */
		for (Entity part : multipart.getBodyParts()) {

			/*
			 * Determine the "boundary" from the multipart parent using the
			 * contentTypeField.
			 */
			contentTypeField = (ContentTypeField) multipart.getParent().getHeader().getField(FieldName.CONTENT_TYPE);

			/*
			 * In front of every boundary the '--' must be specified.
			 * https://tools.ietf.org/html/rfc2046
			 */
			bodyContent.append("--");
			bodyContent.append(contentTypeField.getBoundary());
			bodyContent.append(System.lineSeparator());

			/*
			 * Add unmodified header.
			 */
			bodyContent.append(part.getHeader());
			bodyContent.append(System.lineSeparator());

			/*
			 * Add the recommended part from multipart to the bodyContent.
			 */
			createBody(part);
		}

		/*
		 * In front of every boundary the '--' must be specified.
		 * https://tools.ietf.org/html/rfc2046
		 */
		bodyContent.append(System.lineSeparator());
		bodyContent.append("--");
		bodyContent.append(contentTypeField.getBoundary());

		/*
		 * At the end of the last boundary the '--' must be specified.
		 * https://tools.ietf.org/html/rfc2046
		 */
		bodyContent.append("--");
		bodyContent.append(System.lineSeparator());

		/*
		 * If available, add "epilogue" after the multipart messages.
		 */
		if (null != multipart.getEpilogue()) {
			bodyContent.append(multipart.getEpilogue());
			bodyContent.append(System.lineSeparator());
		}
	}

	/**
	 * Initialize global variables.
	 */
	private void initGlobalVariables() {

		mailFrom = null;
		footerAvailableResult = null;

		parseContent.delete(0, parseContent.length());

		message = null;
		body = null;
		bodyContent.delete(0, bodyContent.length());
		contentTypeField = null;

		try {
			argsBean.init();
		} catch (FooterMilterException eFooterMilterException) {
			log.error(
					"***** Program stop, because FooterMilter detects a runtime error! ***** (For more details, see error messages and caused by below).");
			log.error("FooterMilterException                   : " + eFooterMilterException);
			log.error(ExceptionUtils.getStackTrace(eFooterMilterException));
		}

		entityTextBody = null;

		bodyModified = null;

		addHeaderContent.delete(0, addHeaderContent.length());
	}

	/**
	 * Log MilterContext (context) default (0) part.
	 * 
	 * @param context
	 */
	private void logContext(MilterContext context) {
		logContext(context, 0);
	}

	/**
	 * Log MilterContext (context) SMFIC or default (0) part.
	 * 
	 * @param context
	 * @param smfic
	 */
	private void logContext(MilterContext context, int smfic) {

		switch (smfic) {
		case CommandProcessor.SMFIC_CONNECT:
			if (context.getMacros(CommandProcessor.SMFIC_CONNECT) != null) {
				log.debug("*context.getMacros(SMIFC_CONNECT)       : "
						+ context.getMacros(CommandProcessor.SMFIC_CONNECT));

				if (context.getMacros(CommandProcessor.SMFIC_CONNECT).containsKey("v")) {
					log.debug("*context.getMacros(SMIFC_CONNECT)|(\"v\") : "
							+ context.getMacros(CommandProcessor.SMFIC_CONNECT).get("v").toString());
				}
				if (context.getMacros(CommandProcessor.SMFIC_CONNECT).containsKey("{daemon_name}")) {
					log.debug("*context.getMacros(SMIFC_CONNECT)|(\"{...: "
							+ context.getMacros(CommandProcessor.SMFIC_CONNECT).get("{daemon_name}").toString());
				}
				if (context.getMacros(CommandProcessor.SMFIC_CONNECT).containsKey("j")) {
					log.debug("*context.getMacros(SMIFC_CONNECT)|(\"j\") : "
							+ context.getMacros(CommandProcessor.SMFIC_CONNECT).get("j").toString());
				}
			}

			break;
		case CommandProcessor.SMFIC_HELO:
			if (context.getMacros(CommandProcessor.SMFIC_HELO) != null) {
				log.debug(
						"*context.getMacros(SMIFC_HELO)          : " + context.getMacros(CommandProcessor.SMFIC_HELO));
			}

			break;
		case CommandProcessor.SMFIC_MAIL:
			if (context.getMacros(CommandProcessor.SMFIC_MAIL) != null) {
				log.debug(
						"*context.getMacros(SMIFC_MAIL)          : " + context.getMacros(CommandProcessor.SMFIC_MAIL));

				if (context.getMacros(CommandProcessor.SMFIC_MAIL).containsKey("{mail_host}")) {
					log.debug("*context.getMacros(SMIFC_MAIL)|(\"{mai...: "
							+ context.getMacros(CommandProcessor.SMFIC_MAIL).get("{mail_host}").toString());
				}
				if (context.getMacros(CommandProcessor.SMFIC_MAIL).containsKey("{mail_mailer}")) {
					log.debug("*context.getMacros(SMIFC_MAIL)|(\"{mai...: "
							+ context.getMacros(CommandProcessor.SMFIC_MAIL).get("{mail_mailer}").toString());
				}
				if (context.getMacros(CommandProcessor.SMFIC_MAIL).containsKey("{mail_addr}")) {
					log.debug("*context.getMacros(SMIFC_MAIL)|(\"{mai...: "
							+ context.getMacros(CommandProcessor.SMFIC_MAIL).get("{mail_addr}").toString());
				}
			}

			break;
		case CommandProcessor.SMFIC_RCPT:
			if (context.getMacros(CommandProcessor.SMFIC_RCPT) != null) {
				log.debug(
						"*context.getMacros(SMIFC_RCPT)          : " + context.getMacros(CommandProcessor.SMFIC_RCPT));

				if (context.getMacros(CommandProcessor.SMFIC_RCPT).containsKey("{rcpt_mailer}")) {
					log.debug("*context.getMacros(SMIFC_RCPT)|(\"{rcp...: "
							+ context.getMacros(CommandProcessor.SMFIC_RCPT).get("{rcpt_mailer}").toString());
				}
				if (context.getMacros(CommandProcessor.SMFIC_RCPT).containsKey("{rcpt_addr}")) {
					log.debug("*context.getMacros(SMIFC_RCPT)|(\"{rcp...: "
							+ context.getMacros(CommandProcessor.SMFIC_RCPT).get("{rcpt_addr}").toString());
				}
				if (context.getMacros(CommandProcessor.SMFIC_RCPT).containsKey("{rcpt_host}")) {
					log.debug("*context.getMacros(SMIFC_RCPT)|(\"{rcp...: "
							+ context.getMacros(CommandProcessor.SMFIC_RCPT).get("{rcpt_host}").toString());
				}
				if (context.getMacros(CommandProcessor.SMFIC_RCPT).containsKey("i")) {
					log.debug("*context.getMacros(SMIFC_RCPT)|(\"i\")    : "
							+ context.getMacros(CommandProcessor.SMFIC_RCPT).get("i").toString());
				}
			}

			break;
		case CommandProcessor.SMFIC_DATA:
			if (context.getMacros(CommandProcessor.SMFIC_DATA) != null) {
				log.debug(
						"*context.getMacros(SMIFC_DATA)          : " + context.getMacros(CommandProcessor.SMFIC_DATA));

				if (context.getMacros(CommandProcessor.SMFIC_DATA).containsKey("i")) {
					log.debug("*context.getMacros(SMIFC_DATA)|(\"i\")    : "
							+ context.getMacros(CommandProcessor.SMFIC_DATA).get("i").toString());
				}
			}

			break;
		case CommandProcessor.SMFIC_HEADER:
			if (context.getMacros(CommandProcessor.SMFIC_HEADER) != null) {
				log.debug("*context.getMacros(SMFIC_HEADER)        : "
						+ context.getMacros(CommandProcessor.SMFIC_HEADER));

				if (context.getMacros(CommandProcessor.SMFIC_HEADER).containsKey("i")) {
					log.debug("*context.getMacros(SMIFC_HEADER)|(\"i\")  : "
							+ context.getMacros(CommandProcessor.SMFIC_HEADER).get("i").toString());
				}
			}

			break;
		case CommandProcessor.SMFIC_EOH:
			if (context.getMacros(CommandProcessor.SMFIC_EOH) != null) {
				log.debug("*context.getMacros(SMFIC_EOH)           : " + context.getMacros(CommandProcessor.SMFIC_EOH));

				if (context.getMacros(CommandProcessor.SMFIC_EOH).containsKey("i")) {
					log.debug("*context.getMacros(SMIFC_EOH)|(\"i\")     : "
							+ context.getMacros(CommandProcessor.SMFIC_EOH).get("i").toString());
				}
			}

			break;
		case CommandProcessor.SMFIC_BODY:
			if (context.getMacros(CommandProcessor.SMFIC_BODY) != null) {
				log.debug(
						"*context.getMacros(SMIFC_BODY)          : " + context.getMacros(CommandProcessor.SMFIC_BODY));

				if (context.getMacros(CommandProcessor.SMFIC_BODY).containsKey("i")) {
					log.debug("*context.getMacros(SMIFC_BODY)|(\"i\")    : "
							+ context.getMacros(CommandProcessor.SMFIC_BODY).get("i").toString());
				}
			}

			break;
		case CommandProcessor.SMFIC_BODYEOB:
			if (context.getMacros(CommandProcessor.SMFIC_BODYEOB) != null) {
				log.debug("*context.getMacros(SMIFC_BODYEOB)       : "
						+ context.getMacros(CommandProcessor.SMFIC_BODYEOB));

				if (context.getMacros(CommandProcessor.SMFIC_BODYEOB).containsKey("i")) {
					log.debug("*context.getMacros(SMIFC_BODYEOB)|(\"i\") : "
							+ context.getMacros(CommandProcessor.SMFIC_BODYEOB).get("i").toString());
				}
			}

			break;
		case CommandProcessor.SMFIC_ABORT:
			if (context.getMacros(CommandProcessor.SMFIC_ABORT) != null) {
				log.debug(
						"*context.getMacros(SMIFC_ABORT)         : " + context.getMacros(CommandProcessor.SMFIC_ABORT));

				if (context.getMacros(CommandProcessor.SMFIC_ABORT).containsKey("i")) {
					log.debug("*context.getMacros(SMIFC_ABORT)|(\"i\")   : "
							+ context.getMacros(CommandProcessor.SMFIC_ABORT).get("i").toString());
				}
			}

			break;
		case CommandProcessor.SMFIC_OPTNEG:
			if (context.getMacros(CommandProcessor.SMFIC_OPTNEG) != null) {
				log.debug("*context.getMacros(SMFIC_OPTNEG)        : "
						+ context.getMacros(CommandProcessor.SMFIC_OPTNEG));

				if (context.getMacros(CommandProcessor.SMFIC_OPTNEG).containsKey("i")) {
					log.debug("*context.getMacros(SMFIC_OPTNEG)|(\"i\")  : "
							+ context.getMacros(CommandProcessor.SMFIC_OPTNEG).get("i").toString());
				}
			}

			break;
		case CommandProcessor.SMFIC_UNKNOWN:
			if (context.getMacros(CommandProcessor.SMFIC_UNKNOWN) != null) {
				log.debug("*context.getMacros(SMFIC_UNKNOWN)       : "
						+ context.getMacros(CommandProcessor.SMFIC_UNKNOWN));
			}

			break;
		default:
			if (context != null) {
				log.debug("*context.getMtaProtocolVersion()        : " + context.getMtaProtocolVersion());
				log.debug("*context.getSessionProtocolVersion()    : " + context.getSessionProtocolVersion());
				log.debug("*ontextt.milterProtocolVersion()        : " + context.milterProtocolVersion());
				log.debug("*context.PROTOCOL_VERSION               : " + MilterContext.PROTOCOL_VERSION);
				log.debug("*context.getMacros(ttl)                 : " + context.getMacros(ttl));
				log.debug("*context.getMacros(timeout)             : " + context.getMacros(timeout));
				log.debug("*context.getMtaActions()                : " + context.getMtaActions());
				log.debug("*context.getMtaProtocolSteps()          : " + context.getMtaProtocolSteps());
				log.debug("*context.getSessionProtocolSteps()      : " + context.getSessionProtocolSteps());
				log.debug("*context.getSessionState()              : " + context.getSessionState());
				log.debug("*context.id()                           : " + context.id());
				log.debug("*context.milterActions()                : " + context.milterActions());
				log.debug("*context.milterProtocolSteps()          : " + context.milterProtocolSteps());
			}

			break;
		}

	}

}
