/**
 * Copyright (c) 2022 Klaus Tachtler. All Rights Reserved.
 * Klaus Tachtler. <klaus@tachtler.net>
 * http://www.tachtler.net
 */
package net.tachtler.jmilter.FooterMilter;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/*******************************************************************************
 * Exception for JMilter.
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
public class FooterMilterException extends Exception {

	private static Logger log = LogManager.getLogger();

	/**
	 * Serial ID.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Constructor:
	 */
	public FooterMilterException() {
		super();
	}

	/**
	 * @param message
	 */
	public FooterMilterException(String message) {
		super(message);

		log.error("Exception: " + message);
	}

	/**
	 * @param cause
	 */
	public FooterMilterException(Throwable cause) {
		super(cause);

		log.error("Caused by: " + ExceptionUtils.getStackTrace(getCause()));
	}

	/**
	 * @param message
	 * @param cause
	 */
	public FooterMilterException(String message, Throwable cause) {
		super(message, cause);
		log.error("Exception: " + message);
		log.error("Caused by: " + ExceptionUtils.getStackTrace(getCause()));
	}

	/**
	 * @param message
	 * @param cause
	 * @param enableSuppression
	 * @param writableStackTrace
	 */
	public FooterMilterException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);

		log.error("Exception: " + message);
		log.error("Caused by: " + ExceptionUtils.getStackTrace(getCause()));
	}

	/**
	 * Throws a initialization or a runtime exception, with message.
	 * 
	 * @param init
	 * @param message
	 */
	public FooterMilterException(Boolean init, String message) {
		super(message);

		InitException(init);

		log.error("Exception: " + message);
	}

	/**
	 * Throws a initialization or a runtime exception, with cause.
	 * 
	 * @param init
	 * @param cause
	 */
	public FooterMilterException(Boolean init, Throwable cause) {
		super(cause);

		InitException(init);

		log.error("Caused by: " + ExceptionUtils.getStackTrace(getCause()));
	}

	/**
	 * Throws a initialization or a runtime exception, with message and cause.
	 * 
	 * @param init
	 * @param message
	 * @param cause
	 */
	public FooterMilterException(Boolean init, String message, Throwable cause) {
		super(message, cause);

		InitException(init);

		log.error("Exception: " + message);
		log.error("Caused by: " + ExceptionUtils.getStackTrace(getCause()));
	}

	/**
	 * Log a initialization or a runtime message.
	 * 
	 * @param init
	 */
	public static void InitException(Boolean init) {
		if (init) {
			log.error(
					"***** Program stop, because FooterMilter could not be initialized! ***** (For more details, see error messages and caused by below).");
		} else {
			log.error(
					"***** Program stop, because FooterMilter detects a runtime error! ***** (For more details, see error messages and caused by below).");
		}
	}

}
