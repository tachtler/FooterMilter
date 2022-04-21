/**
 * Copyright (c) 2022 Klaus Tachtler. All Rights Reserved.
 * Klaus Tachtler. <klaus@tachtler.net>
 * http://www.tachtler.net
 */
package net.tachtler.jmilter.FooterMilter;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;

/*******************************************************************************
 * Bean for JMilter.
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
public class FooterMilterInitBean {

	/**
	 * Returns the IPv4-Address.
	 */
	private InetAddress inetAddress;

	/**
	 * Returns the Port.
	 */
	private int port;

	/**
	 * Return HashMap with mail from as key and text footer as value pair.
	 */
	private HashMap<String, String> mapText = new HashMap<String, String>();

	/**
	 * Return HashMap with mail from as key and html footer as value pair.
	 */
	private HashMap<String, String> mapHtml = new HashMap<String, String>();

	/**
	 * Constructor.
	 */
	public FooterMilterInitBean(InetAddress inetAddress, int port, HashMap<String, String> mapText,
			HashMap<String, String> mapHtml) {
		super();
		this.inetAddress = inetAddress;
		this.port = port;
		this.mapText.clear();
		this.mapHtml.clear();
	}

	/**
	 * Initialize all variables to default or unseeded values.
	 * 
	 * @throws FooterMilterException
	 */
	public final void init() throws FooterMilterException {
		try {
			this.inetAddress = InetAddress.getByName("127.0.0.1");
		} catch (UnknownHostException eUnknownHostException) {
			throw new FooterMilterException(true, eUnknownHostException);
		}

		this.port = 10099;
	}

	/**
	 * @return the inetAddress
	 */
	public InetAddress getInetAddress() {
		return inetAddress;
	}

	/**
	 * @param inetAddress the inetAddress to set
	 */
	public void setInetAddress(InetAddress inetAddress) {
		this.inetAddress = inetAddress;
	}

	/**
	 * @return the port
	 */
	public int getPort() {
		return port;
	}

	/**
	 * @param port the port to set
	 */
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * @return the mapText
	 */
	public HashMap<String, String> getMapText() {
		return mapText;
	}

	/**
	 * @param mapText the mapText to set
	 */
	public void setMapText(HashMap<String, String> mapText) {
		this.mapText = mapText;
	}

	/**
	 * @return the mapHtml
	 */
	public HashMap<String, String> getMapHtml() {
		return mapHtml;
	}

	/**
	 * @param mapHtml the mapHtml to set
	 */
	public void setMapHtml(HashMap<String, String> mapHtml) {
		this.mapHtml = mapHtml;
	}

}
