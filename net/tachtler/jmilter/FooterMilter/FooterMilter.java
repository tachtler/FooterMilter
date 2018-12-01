/**
 * Copyright (c) 2018 Klaus Tachtler. All Rights Reserved.
 * Klaus Tachtler. <klaus@tachtler.net>
 * http://www.tachtler.net
 */
package net.tachtler.jmilter.FooterMilter;

import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nightcode.common.service.ServiceManager;
import org.nightcode.milter.MilterHandler;
import org.nightcode.milter.config.GatewayConfig;
import org.nightcode.milter.net.MilterChannelHandler;
import org.nightcode.milter.net.MilterGatewayManager;
import org.nightcode.milter.util.Actions;
import org.nightcode.milter.util.ProtocolSteps;

/*******************************************************************************
 * JMilter Server for connections from an MTA.
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
public class FooterMilter {

	private static Logger log = LogManager.getLogger();

	/**
	 * Constructor.
	 */
	public FooterMilter() {
		super();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws FooterMilterException {

		/*
		 * Read the arguments from command line into the dwuaFileBean.
		 */
		FooterMilterInitBean argsBean = new FooterMilterInitBean(null, 0, null, null, null, null);

		try {
			argsBean = FooterMilterCLIArgsParser.readArgs(argsBean, args);
		} catch (ParseException eParseException) {
			log.error(
					"***** Program stop, because FooterMilter could not be initialized! ***** (For more details, see error messages and caused by below).");
			log.error("ParseException                          : " + eParseException);
			log.error(ExceptionUtils.getStackTrace(eParseException));
			throw new FooterMilterException(
					"***** Program stop, because FooterMilter could not be initialized! ***** (For more details, see error messages and caused by below).",
					eParseException);
		}

		/*
		 * Start JMilter only, if all required arguments are set.
		 */
		if (argsBean.getInetAddress() != null && argsBean.getPort() != 0 && argsBean.getTcpLoggingEnabled() != null
				&& argsBean.getTcpLogLevel() != null) {
			GatewayConfig gatewayConfig = new GatewayConfig();
			gatewayConfig.setAddress(argsBean.getInetAddress().getHostAddress());
			gatewayConfig.setPort(argsBean.getPort());
			gatewayConfig.setTcpLoggingEnabled(argsBean.getTcpLoggingEnabled());
			gatewayConfig.setTcpLogLevel(argsBean.getTcpLogLevel());

			// Indicates what changes will be made with the messages.
			Actions milterActions = Actions.builder().replaceBody().addHeader().build();

			// Indicates which steps will be skipped.
			ProtocolSteps milterProtocolSteps = ProtocolSteps.builder().build();

			// Create the JMilter handler.
			MilterHandler milterHandler = new FooterMilterHandler(milterActions, milterProtocolSteps, argsBean);

			MilterGatewayManager gatewayManager = new MilterGatewayManager(gatewayConfig,
					() -> new MilterChannelHandler(milterHandler), ServiceManager.instance());

			gatewayManager.start();
		}

	}

}
