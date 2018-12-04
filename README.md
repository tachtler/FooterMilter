# FooterMilter
Java implementation of the Sendmail Milter protocol based on the project of org.nightcode.jmilter from dmitry@nightcode.org to **insert a footer/disclaimer at the end of the body of an email**.

## DokuWiki - Detailed description
The following DokuWiki contains detailed instructions for installation and configuration. It describes the use under [CentOS](https://www.centos.org/) **7** in combination with the MTA [Postfix](http://www.postfix.org/). (The current documentation inside the DokuWiki is **only available in German language**.)

[Tachtler's DokuWiki - Postfix CentOS 7 - FooterMilter einbinden (footermilter)](https://dokuwiki.tachtler.net/doku.php?id=tachtler:postfix_centos_7_-_footermilter_einsetzen_footermilter)

## Installation and Configuration
The description of the installation and configuration of **FooterMilter** is divided into the following parts sections:

 1. [Prerequisites](README.md#prerequisites)
 2. [Download](README.md#download)
 3. [Installation](README.md#installation)
 4. [Configuration](README.md#configuration)
     - [Destination directory: /opt](README.md#destination-directory-opt)
     - [footermilter.service: Set up service / Deamon start](README.md#footermilterservice-set-up-service--deamon-start)
     - [log4j2.xml - Log directory: /var/log/FooterMilter](README.md#log4j2xml---log-directory-varlogfootermilter)
     - [Main configuration file: footermilter.ini](README.md#main-configuration-file-footermilterini)
        - [Section: [service]](README.md#section-service)
        - [Section: [footer]](README.md#section-footer)
 5. [footerMilter: First Start](README.md#footermilter-first-start)
 6. 

### Prerequisites
There is only one dependency for the execution of FooterMilter
 * Running **Java** installation e.g. [OpenJDK](https://openjdk.java.net/) **from version 1.8** or higher
 
### Download
Under the following link, you can download the executable **Java** - `FooterMilter.jar` archive file **and the required dependencies as a package**:
 * [GitHub - tachtler/FooterMilter](https://github.com/tachtler/FooterMilter)

### Installation
To install the **FooterMilter and the required dependencies as a package**, the following instructions must be followed:

:exclamation: **Note** - The FooterMilter and its required dependencies are installed in the following **example** in the 
 * `/opt/FooterMilter`
 
directory.

Download the **FooterMilter and the required dependencies as a package** using following command into the `/tmp` directory:

` # wget -P /tmp https://github.com/tachtler/FooterMilter/archive/master.zip`

After downloading the **FooterMilter archive** into the `/tmp` directory, you can now **switch to** the `/tmp` **directory** using the following command:

`# cd /tmp`

Afterwards you can **unzip** the previously downloaded file `/tmp/master.zip` with the following command:

`# unzip /tmp/master.zip`

which creates the following **directory structure**, which can be listed with the following command:

```
# ls -l /tmp/FooterMilter-master/
total 56
-rw-r--r-- 1 root root  5741 Dec  4 07:00 footermilter.ini
-rw-r--r-- 1 root root 18871 Dec  4 07:00 FooterMilter.jar
-rw-r--r-- 1 root root   288 Dec  4 07:00 footermilter.service
drwxr-xr-x 2 root root  4096 Dec  4 07:00 lib
-rw-r--r-- 1 root root 11357 Dec  4 07:00 LICENSE
-rw-r--r-- 1 root root  1676 Dec  4 07:00 log4j2.xml
drwxr-xr-x 2 root root    24 Dec  4 07:00 META-INF
drwxr-xr-x 3 root root    21 Dec  4 07:00 net
-rw-r--r-- 1 root root   864 Dec  4 07:00 README.md
```
Whether the downloaded **Java** - `FooterMilter.jar` archive file is **executable** can be **tested with the following command** and should produce an output like the following one:

```
# java -jar /tmp/FooterMilter-master/FooterMilter.jar -h
usage: /path/to/java -jar /path/to/FooterMilter.jar
       [-c <path and name of the config file>] [-h] [-v] [-d]

FooterMilter for Sendmail or Postfix to insert a footer at the end of the body.

 -c,--config <arg>   [REQUIRED] Path and name of the config file
 -d,--debug          DEBUG mode with runtime output
 -h,--help           Print this usage information
 -v,--version        Version of the program

Copyright (c) 2018 Klaus Tachtler, <klaus@tachtler.net>.
All Rights Reserved.
Version 1.0.
```

:exclamation: **IMPORTANT** - The **Java** - `FooterMilter.jar` archive file can **ONLY be executed** if the other files are also present in the described directory structure, **especially the directory** `lib` **and it's content, must be in the same directory !**

:exclamation: **NOTE** - The directory `/tmp/FooterMilter-master/` can, of course, be

 * **renamed** or also
 * be **moved**

**as long as the directory structure** within which the **Java** - `FooterMilter.jar` archive file is located **is completely preserved !** 

### Configuration

#### Destination directory: /opt 

For file system hygiene, the **complete directory** in which the **FooterMilter** is executed should be moved from the `/tmp` directory to a better location for permanent storage, here: `/opt`. The name should also be changed at the same time, which can be done with the following command:


`# mv /tmp/FooterMilter-master /opt/FooterMilter`

:exclamation: **IMPORTANT**

  - If you want to use a **target directory other than** `/opt`,
  - or the **directory name is NOT** `FooterMilter` **to be named,**

must have the file

  * `footermilter.service`

**also be adapted !!!**

#### footermilter.service: Set up service / Deamon start

Before you start the configuration of the contents, you can use the **Start/Stopp/Restart-Script** included in the **Package** with the designation

  * `footermilter.service`

the automatic start/stop/restart of the **FooterMilter** under **systemd**.

To do this, first copy the **Start/Stopp/Restart script** with the following command to the directory
 
  * `/usr/lib/systemd/system`
  
can be moved:

`# mv /opt/footerMilter/footermilter.service /usr/lib/system/system/footermilter.service`

Then the following command must be used to announce the new script to the **systemd**:

`# systemctl daemon-reload`
 
In order to have **FooterMilter**, which runs as `service/deamon` as **background process**, available even after a restart of the server, the `service/deamon` should be started with the server, which can be realized with the following command:

```
# systemctl enable footermilter.service`
Created symlink from /etc/systemd/system/multi-user.target.wants/footermilter.service to /usr/lib/systemd/system/footermilter.service.
```

A check whether the `footermilter.service` service is really started when the server is restarted can be done with the following command and should display a message as shown below:

```
# systemctl list-unit-files --type=service | grep -e footermilter.service
footermilter.service enabled
```

or

```
# systemctl is-enabled footermilter.service
enabled
```

#### log4j2.xml - Log directory: /var/log/FooterMilter

The content of the configuration file `/opt/FooterMilter/log4j2.xml` determines to which **directory LOG data should be written**, if necessary.

By **default**, the log data is written to:
 
 * `/var/log/FooterMilter`

These and other settings can be made in this file on the subject of logging. See also the external link:
  * [Log4j - Configuration Log4j 2 - Apache Log4j 2](https://logging.apache.org/log4j/2.x/manual/configuration.html)
 
#### Main configuration file: footermilter.ini

:exclamation: **IMPORTANT** - ** The default configuration file is located under** `/opt/FooterMilter/footermilter.ini`

:exclamation: **IMPORTANT**

  - If you want to use a **target directory other than** `/opt`,
  - or the **directory name is NOT** `FooterMilter` **to be named,**

must have the file

  * `footermilter.service`

**also be adapted !!!**

The main configuration file of the **FooterMilter** - **''footermilter.ini''** includes all settings for
  * the start and its **start parameters** - section `[server]`
  * the configuration of the **footer** section to be used `[footer]`

#### Section: [service]

The following parameters can be set in the section `[server]`:

| Parameter    | Default value | Description                                                                    |
| ------------ | ------------- | ------------------------------------------------------------------------------ |
| `listen`    | `127.0.0.1`  | IPv4-address or hostname where the service/daemon should be reachable          |
| `port`      | `10099`       | Port where the service/daemon should be reachable                              |
| `logging`   | `false`       | Activation of TCP logging from [JMilter](https://github.com/nightcode/jmilter) |
| `loglevel`  | `INFO`        | Log-Level for TCP-Logging of [JMilter](https://github.com/nightcode/jmilter) |

:exclamation: **NOTE** - **Enabling logging activates a TCP log which is very talkative!**

:exclamation: **NOTE** - **This should only be activated in case of connection problems, as the TCP connection data is output here.**

:exclamation: **NOTE** - **The log level should NOT be set to** `DEBUG`, **because NO log data is output here. This only happens with the other possible log levels such as** `INFO` **!**

:exclamation: **NOTE** - **If** `DEBUG` **logging is desired for troubleshooting purposes, this can be achieved with the parameter** `-d` **in the start script or with a manual start.**

#### Section: [footer]

In the `[footer]` section(s) **multiple configurations** are possible and desired, but please note the following:

  - Each section `[footer]` must begin with the word **footer**.
  - **Each section** `[footer]` must have a **unambiguous designation**.

**Examples**:

  * `[footer: default]`
  * `[localpart@domain.tld]`
  * `[footer 001]`
  * `[footer-default]`
  * `[footer]`

The following parameters can be set in the section(s) `[footer]`:

| Parameter   | Default value                                 | Description                                                       |
| ------------| --------------------------------------------- | ----------------------------------------------------------------- |
| `enabled`  | `true`                                       | Should this `[footer]` section be active                         |
| `from`      | `user@example.com` **or** `@example.com` | **E-Mail**-Address **or** **Domain** with **\'@\'**-sign in front  | 
| `text`      | `--`                                         | Footer for the `Content-Type` - `text/plain`                   |
| `html`      | `--`                                         | Footer for the `Content-Type` - `text/html`                    |

The following **special feature** applies to the
 
 * **Parameter:** `from`

If `@Domain.tld` is entered here instead of a complete e-mail address, this **Footer** will be attached if no e-mail address applies to other **Footers**. 

:exclamation: **NOTE** - `@Domain.tld` is the **standard footer for this domain!**

**Examples**:

  * `user@example.com` - e-mail address
  * `@example.com` - Default for this domain if no email address matches.
  * `@sub.example.com` - Default for this sub-domain if no email address matches.

The following **special feature** applies to the

 * **Parameter:** `text` and
 * **Parameter:** `html`

For **better readability** during the configuration of the parameters `text` and `html`, e.g. a

 * `\` **(backslash)**
 
**at the END of a line**.

In addition, subsequent **"Escape" sequences** can be used to increase readability when configuring the respective **Footer**.


| Escape sequence | Description                                                     |
| --------------- | --------------------------------------------------------------- |
| `\t`            | Insert a tabulator into the text at this point.                 |
| `\b`            | Add a "backspace" to the text at this point.                    |
| `\n`            | Add a new line to the text at this point.                       |
| `\r`            | Add a carriage return code to the text at this point.           |
| `\f`            | Add a form feed to the text at this point.                      |
| `\'`            | Insert a single quotation mark into the text at this point.     |
| `\"`            | Insert a double quotation mark in the text at this point.       |
| `\\`            | Add a "backslash" character to the text at this point.          |

**Examples**:

**text/plain**

```
-- \
\n\
\n\
--------------------------------------------\n\
Footer default\n\
--------------------------------------------\n\
\n
```

**text/html**

```
<br\>\n\
<br\>\n\
<span style=\"font-family:monospace; color:#000000\">--&nbsp;</span><br>\n\
<br\>\n\
<span style=\"font-family:monospace; color:#000000\">Footer default</span><br>\n\
<br\>\n\n
```

Finally as an example a **complete configuration file**:

```
################################################################################
# JMilter Server for connections from an MTA to add a footer.
# 
# JMilter is an Open Source implementation of the Sendmail milter protocol, for
# implementing milters in Java that can interface with the Sendmail or Postfix
# MTA.
# 
# Java implementation of the Sendmail Milter protocol based on the project of
# org.nightcode.jmilter from dmitry@nightcode.org.
# 
# @author Klaus Tachtler. <klaus@tachtler.net>
# 
#         Homepage : http://www.tachtler.net
# 
#         Licensed under the Apache License, Version 2.0 (the "License"); you
#         may not use this file except in compliance with the License. You may
#         obtain a copy of the License at
#
#         http://www.apache.org/licenses/LICENSE-2.0
#
#         Unless required by applicable law or agreed to in writing, software
#         distributed under the License is distributed on an "AS IS" BASIS,
#         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
#         implied. See the License for the specific language governing
#         permissions and limitations under the License..
# 
# Copyright (c) 2018 Klaus Tachtler. All Rights Reserved.
# Klaus Tachtler. <klaus@tachtler.net>
# http://www.tachtler.net
#
################################################################################
 
 
################################################################################ 
# [server] section - Start configuration for the server.
################################################################################

[server]

# IPv4 address or hostname to listen.
listen = 127.0.0.1

# Port to listen.
port = 10099

# Enable or disable TCP-Logging by setting the following parameter:
# true|false|yes|no|y|n (case insensitive)
#
# !IMPORTANT: Please set to false, true enables ONLY TCP-Logging see:
#             https://github.com/nightcode/jmilter
#  
#             If you want do DEBUG the FooterMilter.jar itself, please use the
#             parameter --> -d <-- as startup parameter as well!  
#
logging = false

# Set TCP-Logging Log-Level to INFO, WARN, ERROR, TRACE or DEBUG.
# Only relevant if logging = true
#
# !IMPORTANT: Please set to INFO to see the TCP-Logging, DEBUG will produce  
#             NO output. This is only for TCP-Logging see: 
#             https://github.com/nightcode/jmilter
#
loglevel = INFO


################################################################################ 
# [footer] section - Configuration of the footers to be used.
################################################################################
#
# !IMPORTANT: Every section  
#             a.) must start with the word: footer
#             b.) must have an unique name
#
# EXAMPLES:   [footer: default], [footer: localpart@domain.tld], [footer: 001]
#
# ==============================================================================
#             
# from field: If inside a [footer]-section the from field was defined with the
#             following syntax: @domain.tld -> NO localpart! <-
#             this will be used as DEFAULT for the domain, if no email address
#             will match!
#
# EXAMPLE:    from = @example.com
#
# ==============================================================================
#             
# text/html : For a better configuration view for the text and html fields, it's
#             possible to use a single \ (backslash) at the end of the line!
#
# EXAMPLE:    text = -- \
#             first word \
#             second word \
#             last word.
#        
# SAME AS:    text = -- first word second word last word.
#
# ==============================================================================
#             
# escape's  : Some escape sequences can be used, for more well formatted output!
#             Escape sequences description:
#                           
#             \t     Insert a tab in the text at this point.
#             \b     Insert a backspace in the text at this point.
#             \n     Insert a newline in the text at this point.
#             \r     Insert a carriage return in the text at this point.
#             \f     Insert a formfeed in the text at this point.
#             \'     Insert a single quote character in the text at this point.
#             \"     Insert a double quote character in the text at this point.
#             \\     Insert a backslash character in the text at this point.
#
################################################################################

[footer: @example.com]

# Enable this footer.
enabled = true

# Email "mail from:" for generating the footer.
from = @example.com

# Footer for text/plain.
text = -- \
\n\
\n\
--------------------------------------------\n\
Footer default\n\
--------------------------------------------\n\
\n

# Footer for text/html.
html = <br\>\n\
<br\>\n\
<span style=\"font-family:monospace; color:#000000\">--&nbsp;</span><br>\n\
<br\>\n\
<span style=\"font-family:monospace; color:#000000\">Footer default</span><br>\n\
<br\>\n\n

################################################################################

[footer: user@example.com]

# Enable this footer.
enabled = true

# Email "mail from:" for generating the footer.
from = user@example.com

# Footer for text/plain.
text = -- \n\n--------------------------------------------\nFooter user\n--------------------------------------------\n\n

# Footer for text/html.
html = <br\>\n<br\>\n<span style=\"font-family:monospace; color:#000000\">--&nbsp;</span><br>\n<br\>\n<span style=\"font-family:monospace; color:#000000\">Footer user</span><br>\n<br\>\n\n

################################################################################
```

### footerMilter: First Start 

The **FooterMilter** `service/daemon` can be started with the following commands:

`# systemctl start footermilter.service`

The following command can be used to query the status of the **FooterMilter** `service/daemon`:

```
# systemctl status footermilter.service
● footermilter.service - FooterMilter Java Service
   Loaded: loaded (/usr/lib/systemd/system/footermilter.service; enabled; vendor preset: disabled)
   Active: active (running) since Mon 2018-11-26 12:08:42 CET; 4s ago
 Main PID: 12118 (java)
   CGroup: /system.slice/footermilter.service
           └─12118 /usr/bin/java -jar FooterMilter.jar -c footermilter.ini

Nov 26 12:08:42 server70.idmz.tachtler.net systemd[1]: Started FooterMilter J...
Nov 26 12:08:42 server70.idmz.tachtler.net systemd[1]: Starting FooterMilter ...
Nov 26 12:08:43 server70.idmz.tachtler.net java[12118]: Nov 26, 2018 12:08:43...
Nov 26 12:08:43 server70.idmz.tachtler.net java[12118]: INFO: [MilterGatewayM...
Hint: Some lines were ellipsized, use -l to show in full.
```

The following query shows on which IPv4 address and port the **FooterMilter** is listening:

```
# netstat -tulpen | grep java
tcp   0   0 192.168.0.70:10099      0.0.0.0:*          LISTEN      0          56485375   12118/java
```

* _The listening on the IPv4 address_ `192.168.0.70` _and port_ `10099''` _was configured here._

### Postfix Configuration

The following configurations must be performed as a minimum in order to be able to make [Postfix](http://www.postfix.org/)
to be able to access the **FooterMilter** `service/daemon`:

:exclamation: **IMPORTANT** - **Please do not include BEFORE - DKIM - , because otherwise the SIGNATUR will break!**

 
#### /etc/postfix/main.cf

The following change must be made to the [Postfix](http://www.postfix.org/) configuration file:

  * `/etc/postfix/main.cf`

( **Relevant excerpt only** ):

```
# --------------------------------------------------------------------------------
# New - http://www.postfix.org/MILTER_README.html
# MILTER CONFIGURATIONS
# --------------------------------------------------------------------------------

# FooterMilter (footer_milter)
footer_milter = inet:192.168.0.70:10099
```

#### /etc/postfix/master.cf 

The following change must be made to the [Postfix](http://www.postfix.org/) configuration file :

  * `/etc/postfix/master.cf`

( **Relevant excerpt only** ):

```
#
# Postfix master process configuration file.  For details on the format
# of the file, see the master(5) manual page (command: "man 5 master").
#
# Do not forget to execute "postfix reload" after editing this file.
#
# ==========================================================================
# service type  private unpriv  chroot  wakeup  maxproc command + args
#               (yes)   (yes)   (yes)   (never) (100)
# ==========================================================================
smtp      inet  n       -       n       -       -       smtpd
# FooterMilter
   -o smtpd_milters=${footer_milter}
```

