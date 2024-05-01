/**
 * Copyright (c) 2024 Klaus Tachtler. All Rights Reserved.
 * Klaus Tachtler. <klaus@tachtler.net>
 * http://www.tachtler.net
 */
package net.tachtler.jmilter.FooterMilter;

import java.net.InetSocketAddress;

import org.apache.commons.cli.ParseException;

import org.nightcode.milter.Actions;
import org.nightcode.milter.MilterHandler;
import org.nightcode.milter.ProtocolSteps;
import org.nightcode.milter.net.MilterGatewayManager;
import org.nightcode.milter.net.ServerFactory;
import org.nightcode.milter.util.NetUtils;

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
 *         Copyright (c) 2024 by Klaus Tachtler.
 ******************************************************************************/
public class FooterMilter {
	
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
		FooterMilterInitBean argsBean = new FooterMilterInitBean(null, 0, null, null);

		try {
			argsBean = FooterMilterCLIArgsParser.readArgs(argsBean, args);
		} catch (ParseException eParseException) {
			throw new FooterMilterException(true, eParseException);
		}

		/*
		 * Start JMilter only, if all required arguments are set.
		 */
		if (argsBean.getInetAddress() != null && argsBean.getPort() != 0) {
			
			// Variables.
			StringBuffer config = new StringBuffer();
			
			// Build listen address:port variable.
			config.append(argsBean.getInetAddress().getHostAddress());
			config.append(":");
			config.append(argsBean.getPort());
			
			// Generate InetSocketAddress with listen address and port.
			InetSocketAddress address = NetUtils.parseAddress(System.getProperty("jmilter.address", config.toString()));

			// Indicates what changes will be made with the messages.
			Actions milterActions = Actions.builder().replaceBody().addHeader().build();

			// Indicates which steps will be skipped.
			ProtocolSteps milterProtocolSteps = ProtocolSteps.builder().build();

			// Create the ServerFactory with the address and port information.
			ServerFactory<InetSocketAddress> serverFactory = ServerFactory.tcpIpFactory(address);
			
			// Create the JMilter handler.
			MilterHandler milterHandler = new FooterMilterHandler(milterActions, milterProtocolSteps, argsBean);

			// Create the JMilter gatewayManager.
			try (
			MilterGatewayManager<InetSocketAddress> gatewayManager = new MilterGatewayManager<>(serverFactory, milterHandler)) {
				gatewayManager.bind();
			}
					
		}

	}

}
