# Jenkins Publisher plug-in for IBM UrbanCode Deploy
---
Note: This is not the plugin distributable! This is the source code. To find the installable plugin, go to the plug-in page on the [IBM UrbanCode Plug-ins microsite](https://developer.ibm.com/urbancode/plugins).

### License
This plug-in is protected under the [Eclipse Public 1.0 License](http://www.eclipse.org/legal/epl-v10.html)

### Overview
Jenkins is a continuous integration server that supports interactions with other technologies by using a plug-in model.

This plug-in is installed into the Jenkins server and provides the ability to publish artifacts into an IBM UrbanCode Deploy component as a post-build action.

### Documentation
All plug-in documentation is updated and maintained on the [IBM UrbanCode Plug-ins microsite](https://developer.ibm.com/urbancode/plugins).

### Support
Plug-ins downloaded directly from the [IBM UrbanCode Plug-ins microsite](https://developer.ibm.com/urbancode/plugins) are fully supported by IBM. Create a GitHub Issue or Pull Request for minor requests and bug fixes. For time sensitive issues that require immediate assistance, [file a PMR](https://www-947.ibm.com/support/servicerequest/newServiceRequest.action) through the normal IBM support channels. Plug-ins built externally or modified with custom code are supported on a best-effort-basis using GitHub Issues.

### Locally Build the Plug-in
This open source plug-in uses Gradle as its build tool. [Install the latest version of Gradle](https://gradle.org/install) to build the plug-in locally. Build the plug-in by running the `gradle jpi` command in the plug-in's root directory. The plug-in distributable will be placed under the `build/libs` folder.

## Release Notes

### Version 1.7
- Use Global and Alternative credentials appropriately.
- Continue build process if Component Version link assignment fails.

### Version 1.6
- Fixed PI77548 - Unable to resolve component process properties.

### Version 1.5
- Fixed unserializable error when publishing versions.

### Version 1.4
- Added checkbox to configure administrative user.
- Added per job user credential configuration.

### Version 1.3
- Fixed PI61971 - Connection pool leak in Jenkins ibm-ucdeploy-publisher.

### Older Versions
- Fixed PI32899 - Jenkins plugin fails on slave nodes with an UnserializbleException
- Fixed PI36005 - Jenkins plugin 1.2.1 not compatible with builds created with earlier versions of the plugin
- Fixed PI37957 - Pulled in a fix for excludes options not being handled by a common library.
