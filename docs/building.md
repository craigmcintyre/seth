<table style="width:100%;">
  <tr>
    <td style="text-align: left;"><a href="version-history.md">Prev: SETH Version History</a></td>
    <td style="text-align: right;"><a href="running.md">Next: Running SETH</a></td>
  </tr>
  <tr>
    <td colspan="2" style="text-align: center;"><a href="index.md">Contents</a></td>
  </tr>
</table>

# Building SETH

SETH is a Java application, so it can run on any platform that Java runs on. SETH requires version 11 of 
the Java runtime and version 11 of the JDK to build it.

SETH is a Maven project. It can be built with Maven 3.9.x and likely much older versions too. 

You can build SETH using the standard Maven command: `mvn clean package`

This will create a `target` directory containing the built JAR file, `seth-<version>-SNAPSHOT.jar`.
SETH is packaged as a single JAR file with all of its dependencies included.

The main entry point for the application is `com.rapidsdata.seth.Seth`.

<table style="width:100%;">
  <tr>
    <td style="text-align: left;"><a href="version-history.md">Prev: SETH Version History</a></td>
    <td style="text-align: right;"><a href="running.md">Next: Running SETH</a></td>
  </tr>
  <tr>
    <td colspan="2" style="text-align: center;"><a href="index.md">Contents</a></td>
  </tr>
</table>