This plugin requires log4j version 1.2.12 or higher so in order for it to work in Jenkins 1.399 (and maybe future versions)
or older you need to replace the log4j jar bundled with the Jenkins web app if it's older than 1.2.12.
Another option I've seen being suggested is to use the java.endorsed.dirs system property to point to a location where a
newer version of log4j jar exists but I have been unable to make it work.

### Version 1.3

Fixed PI61971 - Connection pool leak in Jenkins ibm-ucdeploy-publisher.

### Version 1.4
Added checkbox to configure administrative user.
Added per job user credential configuration.

### Version 1.5
Fixed unserializable error when publishing versions.

### Version 1.6
Fixed PI77548 - Unable to resolve component process properties.

### Older Versions
Fixed PI32899 - Jenkins plugin fails on slave nodes with an UnserializbleException

Fixed PI36005 - Jenkins plugin 1.2.1 not compatible with builds created with earlier versions of the plugin

Fixed PI37957 - Pulled in a fix for excludes options not being handled by a common library.