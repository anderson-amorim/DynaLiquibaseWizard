package liquibasewizard.wizards;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import liquibasewizard.wizards.bean.ChangeSet;

public class LiquibaseDocument {

	private String author;
	private String inputSqlFilePath;
	private boolean calculateCatalog;

	public LiquibaseDocument(String author, String inputFilePath, boolean calculateCatalog) {
		this.author = author;
		this.inputSqlFilePath = inputFilePath;
		this.calculateCatalog = calculateCatalog;
	}

	/**
	 * Transforma o arquivo SQL em um Documento do Liquibase
	 * 
	 * @return
	 * @throws Exception
	 */
	public InputStream transformToXml() throws Exception {

		List<String> statements = getFileStatements();

		Document doc = getDocument();
		Element databaseChangeLog = getDatabaseChangeLogElement(doc);
		doc.appendChild(databaseChangeLog);

		for (int i = 0; i < statements.size(); i++) {
			String statement = (statements.get(i).trim());
			if (!"commit".equalsIgnoreCase(statement)) {
				ChangeSet changeSet = new ChangeSet(doc, statement, calculateCatalog, i + 1);
				changeSet.setAuthor(author);
				// changeSet.setId((i + 1) + "_" + filename.substring(0, filename.lastIndexOf(".")));
				databaseChangeLog.appendChild(changeSet.getElement());
			}
		}

		return writeXML(doc);
	}

	/**
	 * Grava o xml gerado em um arquivo
	 * 
	 * @param doc
	 * @return
	 * @throws Exception
	 */
	private InputStream writeXML(Document doc) throws Exception {
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
		DOMSource source = new DOMSource(doc);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		StreamResult result = new StreamResult(outputStream);
		transformer.transform(source, result);
		return new ByteArrayInputStream(outputStream.toByteArray());
	}

	/**
	 * Cria o Document principal do xml
	 * 
	 * @return
	 * @throws ParserConfigurationException
	 */
	private Document getDocument() throws ParserConfigurationException {
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
		return docBuilder.newDocument();
	}

	/**
	 * Separa cada comando do arquivo sql em uma lista de statements
	 * 
	 * @return
	 * @throws Exception
	 */
	private List<String> getFileStatements() throws Exception {
		String content = new Scanner(new File(inputSqlFilePath)).useDelimiter("\\Z").next().replaceAll("\\n", "").replaceAll("\\r", "\n\t\t\t");
		List<String> procedures = new ArrayList<String>();
		while (content.toUpperCase().contains("<PROCEDURE>")) {
			String procedure = content.substring(content.toUpperCase().indexOf("<PROCEDURE>"), content.toUpperCase().indexOf("</PROCEDURE>") + 12);
			content = content.replace(content.substring(content.toUpperCase().indexOf("<PROCEDURE>"), content.toUpperCase().indexOf("</PROCEDURE>") + 12), "").trim();
			procedures.add(procedure);
		}
		if (content.toUpperCase().contains("TRIGGER") || content.toUpperCase().contains("PROCEDURE") || content.toUpperCase().contains("FUNCTION")) {
			throw new Exception("Funções PLSQL devem estar entre as tags <procedure></procedure>");
		}
		List<String> statements = new ArrayList<String>();
		statements.addAll(Arrays.asList(content.trim().split(";")));
		statements.addAll(procedures);
		return statements;
	}

	/**
	 * Cria o Element DatabaseChangeLog
	 * 
	 * @param doc
	 * @return
	 */
	private Element getDatabaseChangeLogElement(Document doc) {
		Element databaseChangeLog;
		databaseChangeLog = doc.createElement("databaseChangeLog");
		databaseChangeLog.setAttribute("xmlns", "http://www.liquibase.org/xml/ns/dbchangelog");
		databaseChangeLog.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
		databaseChangeLog.setAttribute("xsi:schemaLocation", "http://www.liquibase.org/xml/ns/dbchangelog http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-3.0.xsd");
		return databaseChangeLog;
	}

	/**
	 * Adiciona o nome do arquivo ao db-changelog.xml que se encontra no container selecionado
	 * 
	 * @param container
	 * @param fileName
	 * @throws Exception
	 */
	public void addToDbChangeLog(String container, String fileName) throws Exception {
		String dbChangelogFilePath = container + File.separator + "db-changelog.xml";
		if (!new File(dbChangelogFilePath).exists()) {
			throw new Exception("Arquivo " + dbChangelogFilePath + " não existe!");
		}
		String content = new Scanner(new File(dbChangelogFilePath), "ISO-8859-2").useDelimiter("\\Z").next().replaceAll("\\n", "").replaceAll("\\r", "\n");

		if (!content.contains(fileName)) {
			String tag = "\n\t<include file=\"" + fileName + "\" relativeToChangelogFile=\"true\" />";
			int ind = content.lastIndexOf("relativeToChangelogFile=\"true\" />");
			content = new StringBuilder(content).replace(ind + 33, ind + 33, tag).toString();
			content.substring(ind + 33);
			PrintWriter out = new PrintWriter(dbChangelogFilePath);
			out.println(content);
			out.close();
		}
	}

}
