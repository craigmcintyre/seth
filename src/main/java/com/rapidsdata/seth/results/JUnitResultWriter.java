// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.results;

import com.rapidsdata.seth.TestResult;
import com.rapidsdata.seth.contexts.AppContext;
import com.rapidsdata.seth.exceptions.SethBrownBagException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;

/**
 * A class that writes JUnit-parsable XML results of the tests to a file.
 *
 * Some resources on writing a valid file are:
 * https://llg.cubic.org/docs/junit/
 * https://github.com/windyroad/JUnit-Schema/blob/d75c30f8c2d878201402999c3d2a421a18a65248/JUnit.xsd
 * https://stackoverflow.com/questions/442556/spec-for-junit-xml-output
 */
public class JUnitResultWriter extends LoggableResultWriter
{
  private static String HOSTNAME = null;

  protected final Path resultPath;

  /**
   * Constructor
   * @param context Common information and settings of the application.
   */
  public JUnitResultWriter(AppContext context, File resultDir, String resultName)
  {
    super(context);
    resultPath = Paths.get(resultDir.getPath(), resultName);
  }

  /**
   * Write out the complete test results to file and the test log.
   * @param results the list of results of each individual test.
   */
  @Override
  public void writeResults(List<TestResult> results)
  {
    // Generate and write the summary to the log.
    ResultSummary summary = summmariseResults(results);
    writeSummary(summary);

    // Generate a JUnit compatible XML result file.

    DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder documentBuilder = null;

    try {
      documentBuilder = documentFactory.newDocumentBuilder();

    } catch (ParserConfigurationException e) {
      e.printStackTrace();
      throw new SethBrownBagException(e);
    }

    Document document = documentBuilder.newDocument();

    // root element
    Element root = document.createElement("testsuite");
    document.appendChild(root);

    root.setAttribute("tests", String.valueOf(summary.getNumTestsExecuted()));
    root.setAttribute("failures", String.valueOf(summary.getNumTestsFailed()));
    root.setAttribute("name", "Seth tests");
    root.setAttribute("time", millisToSeconds(System.currentTimeMillis() - context.getAppStartTime(), 3));
    root.setAttribute("errors", String.valueOf(summary.getNumTestsAborted()));
    root.setAttribute("skipped", String.valueOf(summary.getNumTestsSkipped()));
    root.setAttribute("timestamp", millisToISO8601DateTimePattern(context.getAppStartTime()));
    root.setAttribute("hostname", getHostName());

    for (TestResult testResult : results) {
      Element testCase = document.createElement("testcase");
      root.appendChild(testCase);

      testCase.setAttribute("name", testResult.getTestName() + context.getCommandLineArgs().testSuffix);
      testCase.setAttribute("time", nanosToSeconds(testResult.getExecutionTimeNs(), 3));
      testCase.setAttribute("classname", testResult.getTestFile().getAbsolutePath() + context.getCommandLineArgs().testSuffix);

      switch (testResult.getStatus()) {
        case FAILED:
          Element failureElement = document.createElement("error");
          testCase.appendChild(failureElement);

          if (testResult.getFailureException() != null) {
            failureElement.setAttribute("type", testResult.getFailureException().getClass().getName());
            failureElement.setAttribute("message", "\n" + testResult.getFailureException().getMessage());

          } else {
            failureElement.setAttribute("type", "No failure information provided");
          }
          break;

        case ABORTED:
          Element errorElement = document.createElement("error");
          testCase.appendChild(errorElement);

          if (testResult.getFailureException() != null) {
            errorElement.setAttribute("type", testResult.getFailureException().getClass().getName());
            errorElement.setAttribute("message", "\n" + testResult.getFailureException().getMessage());

          } else {
            errorElement.setAttribute("type", "Test aborted");
            errorElement.setAttribute("message", "The test was aborted, either because it was interrupted or " +
                                                       "because a thread encountered a failure.");
          }
          break;

        case SKIPPED:
          Element skippedElement = document.createElement("skipped");
          testCase.appendChild(skippedElement);
          skippedElement.setAttribute("message", "");
          break;

        case SUCCEEDED:
        case NOT_STARTED:
        case IN_PROGRESS:
        case VALIDATED:
        default:
          break;
      }
    }

    // Write out the XML file.
    // Transform the DOM Object to an XML File
    try {
      TransformerFactory transformerFactory = TransformerFactory.newInstance();
      Transformer transformer = transformerFactory.newTransformer();
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
      transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
      DOMSource domSource = new DOMSource(document);
      StreamResult streamResult = new StreamResult(resultPath.toFile());

      transformer.transform(domSource, streamResult);

    } catch (TransformerException e) {
      e.printStackTrace();
      throw new SethBrownBagException(e);
    }

    context.getLogger().log("Test results have been written out to: " + resultPath.toString(), false);
  }

  private String nanosToSeconds(long nanos, int numDecimalPlaces)
  {
    double seconds = nanos / 1000000000.0d;
    return String.format("%." + numDecimalPlaces + "f", seconds);
  }

  private String millisToSeconds(long millis, int numDecimalPlaces)
  {
    double seconds = millis / 1000.0d;
    return String.format("%." + numDecimalPlaces + "f", seconds);
  }

  private String millisToISO8601DateTimePattern(long millis)
  {
    Instant instant = Instant.ofEpochMilli(millis);
    LocalDateTime date = instant.atZone(ZoneId.systemDefault()).toLocalDateTime();
    return DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss").format(date);
  }

  private String getHostName()
  {
    synchronized (this) {
      if (HOSTNAME != null) {
        return HOSTNAME;
      }
    }

    try {
      Process process = Runtime.getRuntime().exec("hostname");

      try (InputStream inputStream = process.getInputStream();
           Scanner s = new Scanner(inputStream).useDelimiter("\\A")) {

        if (s.hasNext()) {
          String hostname = s.next();
          if (!hostname.isEmpty()) {
            return updateHostname(hostname.trim());
          }
        }

        // fall out and try another method
      }
    } catch (Exception e) {
      // try another method
    }

    try {
      String hostname = InetAddress.getLocalHost().getHostName();
      if (!hostname.isEmpty()) {
        return updateHostname(hostname);
      }

    } catch (UnknownHostException e) {
      // Fall out and try another method
    }

    // last resort
    return updateHostname("localhost");
  }

  private synchronized String updateHostname(String name)
  {
    if (HOSTNAME != null) {
      return HOSTNAME;
    }

    HOSTNAME = name;
    return HOSTNAME;
  }
}
