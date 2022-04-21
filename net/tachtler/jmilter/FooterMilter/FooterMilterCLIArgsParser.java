/**
 * Copyright (c) 2022 Klaus Tachtler. All Rights Reserved.
 * Klaus Tachtler. <klaus@tachtler.net>
 * http://www.tachtler.net
 */
package net.tachtler.jmilter.FooterMilter;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.ini4j.Ini;
import org.ini4j.InvalidFileFormatException;

/*******************************************************************************
 * Command Line Interface Argument Parser and configuration file reader for the
 * JMilter.
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
 *         Copyright (c) 2022 by Klaus Tachtler.
 ******************************************************************************/
public class FooterMilterCLIArgsParser {

	private static Logger log = LogManager.getLogger();

	/**
	 * Constructor.
	 */
	public FooterMilterCLIArgsParser() {
		super();
	}

	protected static FooterMilterInitBean readArgs(FooterMilterInitBean argsBean, String[] args)
			throws ParseException, FooterMilterException {

		log.debug("*args                                   : " + args);

		final String USAGE = "/path/to/java -jar /path/to/FooterMilter.jar \r\n       [-c <path and name of the config file>] [-h] [-v] [-d]";
		final String HEADER = "\r\nFooterMilter for Sendmail or Postfix to insert a footer at the end of the body.\r\n\r\n";
		final String FOOTER = "\r\nCopyright (c) 2022 Klaus Tachtler, <klaus@tachtler.net>.\r\nAll Rights Reserved.\r\nVersion 1.1.\r\n\r\n";

		Options options = new Options();

		options.addOption("h", "help", false, "Print this usage information");
		options.addOption("v", "version", false, "Version of the program");
		options.addOption("d", "debug", false, "DEBUG mode with runtime output");
		options.addOption("c", "config", true, "[REQUIRED] Path and name of the config file");

		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = parser.parse(options, args, false);

		/* -h,--help */
		if (cmd.hasOption("h")) {
			HelpFormatter helpFormatter = new HelpFormatter();
			helpFormatter.setWidth(80);
			helpFormatter.printHelp(USAGE, HEADER, options, FOOTER);
			System.exit(0);
		}

		/* -v,--version */
		if (cmd.hasOption("v")) {
			System.out.println(FOOTER);
			System.exit(0);
		}

		/* -d,--debug */
		if (cmd.hasOption("d")) {
			Configurator.setRootLevel(Level.DEBUG);
		} else {
			Configurator.setRootLevel(Level.INFO);
		}

		/* -c,--config <Path and name of the config file> */
		if (cmd.hasOption("c")) {

			log.debug("*cmd.getOptionValue(\"c\")                : " + cmd.getOptionValue("c"));

			Ini iniConfig = null;
			try {
				iniConfig = new Ini(new FileReader(cmd.getOptionValue("c")));

				/*
				 * Check, set or error on [server] listen.
				 */
				if (isConfigSectionParamValueValid(iniConfig, "server", "listen",
						"<IPv4-Address or Hostname to listen>")) {
					setServerListen(iniConfig, argsBean);
				}

				/*
				 * Check, set or error on [server] port.
				 */
				if (isConfigSectionParamValueValid(iniConfig, "server", "port", "<Port to listen>")) {
					setServerPort(iniConfig, argsBean);
				}

				/*
				 * Read all footer and create two different HashMaps, one for the text/plain
				 * footer and one for the text/html footer, with the specific data stored.
				 */
				createFooterHashMaps(iniConfig, argsBean);

			} catch (FileNotFoundException eFileNotFoundException) {
				throw new FooterMilterException(true,
						"Required parameter -c,--config <Path and name of the config file> could not be found!",
						eFileNotFoundException);
			} catch (InvalidFileFormatException eInvalidFileFormatException) {
				throw new FooterMilterException(true,
						"Required parameter -c,--config <Path and name of the config file> is not in a valid format!",
						eInvalidFileFormatException);
			} catch (IOException eIOException) {
				throw new FooterMilterException(true,
						"Required parameter -c,--config <Path and name of the config file> could not be accessed!",
						eIOException);
			}
		} else {
			new HelpFormatter().printHelp(USAGE, HEADER, options, FOOTER);
			throw new FooterMilterException(true,
					"Required parameter -c,--config <Path and name of the config file> NOT specified!");
		}

		return argsBean;
	}

	/**
	 * Check whether the parameter within the section from the configuration file is
	 * present and has a value.
	 * 
	 * @param iniConfig
	 * @param section
	 * @param param
	 * @param description
	 * @return true
	 * @throws FooterMilterException
	 */
	private static boolean isConfigSectionParamValueValid(Ini iniConfig, String section, String param,
			String description) throws FooterMilterException {

		if (iniConfig.get(section, param) == null) {
			throw new FooterMilterException(true, "Configuration at section [" + section + "] Parameter: " + param + " "
					+ description + " not found in config file!");
		}

		if (iniConfig.get(section, param).isEmpty() || iniConfig.get(section, param).equals("")) {
			throw new FooterMilterException(true, "Configuration at section [" + section + "] Parameter: " + param + " "
					+ description + " found with empty value!");
		}

		log.debug("*[section]                              : " + "[" + section + "]");
		log.debug("*parameter = *value                     : " + param + " = " + iniConfig.get(section, param));
		log.debug("*description                            : " + description);

		return true;
	}

	/**
	 * Set the listen parameter from the server section of the configuration file to
	 * the argsBean (FooterMilterInitBean).
	 * 
	 * @param iniConfig
	 * @param argsBean
	 * @throws FooterMilterException
	 */
	private static void setServerListen(Ini iniConfig, FooterMilterInitBean argsBean) throws FooterMilterException {

		try {
			argsBean.setInetAddress(InetAddress.getByName(iniConfig.get("server", "listen")));
		} catch (UnknownHostException eUnknownHostException) {
			throw new FooterMilterException(true,
					"Configuration at section [server] Parameter: listen <IPv4-Address or Hostname to listen> is NOT a valid IPv4 address or hostname!",
					eUnknownHostException);
		}

	}

	/**
	 * Set the port parameter from the server section of the configuration file to
	 * the argsBean (FooterMilterInitBean).
	 * 
	 * @param iniConfig
	 * @param argsBean
	 * @throws FooterMilterException
	 */
	private static void setServerPort(Ini iniConfig, FooterMilterInitBean argsBean) throws FooterMilterException {

		int port = 0;

		try {
			port = Integer.parseInt(iniConfig.get("server", "port"));
		} catch (NumberFormatException eNumberFormatException) {
			throw new FooterMilterException(true,
					"Configuration at section [server] Parameter: port <Port to listen> was NOT a valid number, between 1 and 65535!",
					eNumberFormatException);
		}

		if (port >= 1 && port <= 65535) {
			argsBean.setPort(port);
		} else {
			throw new FooterMilterException(true,
					"Configuration at section [server] Parameter: port <Port to listen> was NOT a valid port number, between 1 and 65535!");
		}
	}

	/**
	 * Read all footer and create two different HashMaps, one for the text/plain
	 * footer and one for the text/html footer, with the specific data stored.
	 * 
	 * @param iniConfig
	 * @param argsBean
	 * @throws FooterMilterException
	 */
	private static void createFooterHashMaps(Ini iniConfig, FooterMilterInitBean argsBean)
			throws FooterMilterException {

		Boolean enabled = null;
		String from = null;
		HashMap<String, String> mapText = new HashMap<String, String>();
		HashMap<String, String> mapHtml = new HashMap<String, String>();

		for (Ini.Section section : iniConfig.values()) {

			if (section.getName().toLowerCase().startsWith("footer", 0)) {

				log.debug("----------------------------------------: ");
				log.debug("[section]                               : " + "[" + section.getName() + "]");

				/*
				 * Set the variable enabled and from to null, to determine if a parameter
				 * enabled is inside the footer section and the from parameter was NOT empty
				 * too.
				 */
				enabled = null;
				from = null;

				for (String option : section.keySet()) {

					log.debug("- paramater = value                     : " + option + " = " + section.fetch(option));

					/*
					 * Check if the read parameter is valid.
					 */
					footerIsParameterValid(section.getName(), option);

					/*
					 * Check if the following footer is valid and enabled. Is so, set the variable
					 * enabled for this iteration to true, else set the variable enabled to false
					 * for this iteration.
					 */
					if (option.equalsIgnoreCase("enabled")) {
						if (footerIsEnabled(section.getName(), option, section.fetch(option))) {
							enabled = true;
						} else {
							enabled = false;
						}
					}

					/*
					 * If the variable enabled is true the other footer fields will be written to
					 * the HashMaps. If NOT, the other footer fields will be ignored.
					 */
					if (enabled != null) {
						if (enabled) {

							/*
							 * Save the from field to the variable from for this iteration.
							 */
							if (option.equalsIgnoreCase("from")) {
								if (!section.fetch(option).isEmpty() && !section.fetch(option).equals("")
										&& section.fetch(option) != null) {
									from = section.fetch(option);
								} else {
									throw new FooterMilterException(true, "Configuration at section ["
											+ section.getName() + "] Parameter: from has an empty value!");
								}
							}

							/*
							 * Put the text value with the from field to the HashMap mapText.
							 */
							if (option.equalsIgnoreCase("text")) {
								mapText.put(from, section.fetch(option));
							}

							/*
							 * Put the html value with the from field to the HashMap mapHtml.
							 */
							if (option.equalsIgnoreCase("html")) {
								mapHtml.put(from, section.fetch(option));
							}

						}

					} else {
						throw new FooterMilterException(true, "Configuration at section [" + section.getName()
								+ "] Parameter: enabled is NOT specified!");
					}
				}

				log.debug("----------------------------------------: ");
			}

		}

		/*
		 * Save the temporary generated HashMaps mapText and mapHtml to the Bean
		 * (FooterMilterInitBean).
		 */
		argsBean.setMapText(mapText);
		argsBean.setMapHtml(mapHtml);

		log.debug("----------------------------------------: ");

		mapText.forEach((key, value) -> {
			log.debug("*mapText (key)                          : " + key);
			log.debug("*mapText (value) < Start at next line > : " + System.lineSeparator() + value);
		});

		log.debug("----------------------------------------: ");

		mapHtml.forEach((key, value) -> {
			log.debug("*mapHtml (key)                          : " + key);
			log.debug("*mapHtml (value) < Start at next line > : " + System.lineSeparator() + value);
		});

		log.debug("----------------------------------------: ");

	}

	/**
	 * Check whether the parameter read is a valid parameter.
	 * 
	 * @param section
	 * @param param
	 * @throws FooterMilterException
	 */
	private static void footerIsParameterValid(String section, String param) throws FooterMilterException {
		if (!param.equalsIgnoreCase("enabled") && !param.equalsIgnoreCase("from") && !param.equalsIgnoreCase("text")
				&& !param.equalsIgnoreCase("html")) {
			throw new FooterMilterException(true, "Configuration at section [" + section + "] Parameter: " + param
					+ " is not a valid parameter! (Possible parameters are: enabled, from, text, html) ONLY!");
		}
	}

	/**
	 * Check if inside a footer section the parameter enabled is valid and determine
	 * if the value is positive or negative. If the result is positive the footer
	 * section was enabled and the following footer can be used.
	 * 
	 * @param section
	 * @param param
	 * @param value
	 * @return Boolean
	 * @throws FooterMilterException
	 */
	private static Boolean footerIsEnabled(String section, String param, String value) throws FooterMilterException {

		Boolean result = false;

		if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false") || value.equalsIgnoreCase("yes")
				|| value.equalsIgnoreCase("no") || value.equalsIgnoreCase("y") || value.equalsIgnoreCase("n")) {
			if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes") || value.equalsIgnoreCase("y")) {
				result = true;
			}
		} else {
			throw new FooterMilterException(true, "Configuration at section [" + section + "] Parameter: " + param
					+ " = " + value + " is NOT valid! (Possible values: true|false|yes|no|y|n (case insensitive))");
		}

		return result;
	}

}
