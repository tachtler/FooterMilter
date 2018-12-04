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
