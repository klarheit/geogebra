/* DefaultTeXFontParser.java
 * =========================================================================
 * This file is originally part of the JMathTeX Library - http://jmathtex.sourceforge.net
 *
 * Copyright (C) 2004-2007 Universiteit Gent
 * Copyright (C) 2009 DENIZET Calixte
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or (at
 * your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * A copy of the GNU General Public License can be found in the file
 * LICENSE.txt provided with the source distribution of this program (see
 * the META-INF directory in the source jar). This license can also be
 * found on the GNU website at http://www.gnu.org/licenses/gpl.html.
 *
 * If you did not receive a copy of the GNU General Public License along
 * with this program, contact the lead developer, or write to the Free
 * Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 *
 * Linking this library statically or dynamically with other modules 
 * is making a combined work based on this library. Thus, the terms 
 * and conditions of the GNU General Public License cover the whole 
 * combination.
 * 
 * As a special exception, the copyright holders of this library give you 
 * permission to link this library with independent modules to produce 
 * an executable, regardless of the license terms of these independent 
 * modules, and to copy and distribute the resulting executable under terms 
 * of your choice, provided that you also meet, for each linked independent 
 * module, the terms and conditions of the license of that module. 
 * An independent module is a module which is not derived from or based 
 * on this library. If you modify this library, you may extend this exception 
 * to your version of the library, but you are not obliged to do so. 
 * If you do not wish to do so, delete this exception statement from your 
 * version.
 * 
 */

/* Modified by Calixte Denizet */

package com.himamis.retex.renderer.share;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.himamis.retex.renderer.share.exception.FontAlreadyLoadedException;
import com.himamis.retex.renderer.share.exception.ResourceParseException;
import com.himamis.retex.renderer.share.exception.XMLResourceParseException;
import com.himamis.retex.renderer.share.platform.FontAdapter;
import com.himamis.retex.renderer.share.platform.ParserAdapter;
import com.himamis.retex.renderer.share.platform.Resource;
import com.himamis.retex.renderer.share.platform.font.Font;
import com.himamis.retex.renderer.share.platform.parser.Element;
import com.himamis.retex.renderer.share.platform.parser.NamedNodeMap;
import com.himamis.retex.renderer.share.platform.parser.Node;
import com.himamis.retex.renderer.share.platform.parser.NodeList;

/**
 * Parses the font information from an XML-file.
 */
public class DefaultTeXFontParser {

	/**
	 * if the register font cannot be found, we display an error message but we do it only once
	 */
	private static boolean registerFontExceptionDisplayed = false;
	private static boolean shouldRegisterFonts = true;

	private static interface CharChildParser { // NOPMD
		public void parse(Element el, char ch, FontInfo info) throws XMLResourceParseException;
	}

	private static class ExtensionParser implements CharChildParser {

		ExtensionParser() {
			// avoid generation of access class
		}

		public void parse(Element el, char ch, FontInfo info) throws ResourceParseException {
			int[] extensionChars = new int[4];
			// get required integer attributes
			extensionChars[DefaultTeXFont.REP] = DefaultTeXFontParser.getIntAndCheck("rep", el);
			// get optional integer attributes
			extensionChars[DefaultTeXFont.TOP] = DefaultTeXFontParser.getOptionalInt("top", el,
					DefaultTeXFont.NONE);
			extensionChars[DefaultTeXFont.MID] = DefaultTeXFontParser.getOptionalInt("mid", el,
					DefaultTeXFont.NONE);
			extensionChars[DefaultTeXFont.BOT] = DefaultTeXFontParser.getOptionalInt("bot", el,
					DefaultTeXFont.NONE);

			// parsing OK, add extension info
			info.setExtension(ch, extensionChars);
		}
	}

	private static class KernParser implements CharChildParser {

		KernParser() {
			// avoid generation of access class
		}

		public void parse(Element el, char ch, FontInfo info) throws ResourceParseException {
			// get required integer attribute
			int code = DefaultTeXFontParser.getIntAndCheck("code", el);
			// get required double attribute
			double kernAmount = DefaultTeXFontParser.getFloatAndCheck("val", el);

			// parsing OK, add kern info
			info.addKern(ch, (char) code, kernAmount);
		}
	}

	private static class LigParser implements CharChildParser {

		LigParser() {
			// avoid generation of access class
		}

		public void parse(Element el, char ch, FontInfo info) throws ResourceParseException {
			// get required integer attributes
			int code = DefaultTeXFontParser.getIntAndCheck("code", el);
			int ligCode = DefaultTeXFontParser.getIntAndCheck("ligCode", el);

			// parsing OK, add ligature info
			info.addLigature(ch, (char) code, (char) ligCode);
		}
	}

	private static class NextLargerParser implements CharChildParser {

		NextLargerParser() {
			// avoid generation of access class
		}

		public void parse(Element el, char ch, FontInfo info) throws ResourceParseException {
			// get required integer attributes
			String fontId = DefaultTeXFontParser.getAttrValueAndCheckIfNotNull("fontId", el);
			int code = DefaultTeXFontParser.getIntAndCheck("code", el);

			// parsing OK, add "next larger" info
			info.setNextLarger(ch, (char) code, Font_ID.indexOf(fontId));
		}
	}

	public static final String RESOURCE_NAME = "DefaultTeXFont.xml";

	public static final String STYLE_MAPPING_EL = "TextStyleMapping";
	public static final String SYMBOL_MAPPING_EL = "SymbolMapping";
	public static final String GEN_SET_EL = "GeneralSettings";
	public static final String MUFONTID_ATTR = "mufontid";
	public static final String SPACEFONTID_ATTR = "spacefontid";

	protected static ArrayList<String> Font_ID = new ArrayList<String>();
	private static Map<String, Integer> rangeTypeMappings = new HashMap<String, Integer>();
	private static Map<String, CharChildParser> charChildParsers = new HashMap<String, CharChildParser>();

	private Map<String, CharFont[]> parsedTextStyles;

	private Element root;
	private Object base = null;

	static {
		// string-to-constant mappings
		setRangeTypeMappings();
		// parsers for the child elements of a "Char"-element
		setCharChildParsers();
	}

	private final Resource resource;
	private final ParserAdapter parserAdapter;

	public DefaultTeXFontParser() throws ResourceParseException {
		resource = new Resource();
		parserAdapter = new ParserAdapter();
		Object file = resource.loadResource(DefaultTeXFontParser.class, RESOURCE_NAME);
		try {
			root = parserAdapter.createParserAndParseFile(file, true, true);
		} catch (Exception e) { // JDOMException or IOException
			throw new XMLResourceParseException(RESOURCE_NAME, e);
		}
	}

	public DefaultTeXFontParser(Object file, String name) throws ResourceParseException {
		resource = new Resource();
		parserAdapter = new ParserAdapter();
		try {
			root = parserAdapter.createParserAndParseFile(file, true, true);
		} catch (Exception e) { // JDOMException or IOException
			throw new XMLResourceParseException(name, e);
		}
	}

	public DefaultTeXFontParser(Object base, Object file, String name) throws ResourceParseException {
		this.base = base;
		resource = new Resource();
		parserAdapter = new ParserAdapter();
		try {
			root = parserAdapter.createParserAndParseFile(file, true, true);
		} catch (Exception e) { // JDOMException or IOException
			throw new XMLResourceParseException(name, e);
		}
	}

	private static void setCharChildParsers() {
		charChildParsers.put("Kern", new KernParser());
		charChildParsers.put("Lig", new LigParser());
		charChildParsers.put("NextLarger", new NextLargerParser());
		charChildParsers.put("Extension", new ExtensionParser());
	}

	public FontInfo[] parseFontDescriptions(FontInfo[] fi, Object file, String name)
			throws ResourceParseException {
		if (file == null) {
			return fi;
		}
		ArrayList<FontInfo> res = new ArrayList<FontInfo>(Arrays.asList(fi));
		Element font;
		try {
			font = parserAdapter.createParserAndParseFile(file);
		} catch (Exception e) {
			throw new XMLResourceParseException("Cannot find the file " + name + "!" + e.toString());
		}

		String fontName = getAttrValueAndCheckIfNotNull("name", font);
		// get required integer attribute
		String fontId = getAttrValueAndCheckIfNotNull("id", font);
		if (Font_ID.indexOf(fontId) < 0)
			Font_ID.add(fontId);
		else
			throw new FontAlreadyLoadedException("Font " + fontId + " is already loaded !");
		// get required real attributes
		double space = getFloatAndCheck("space", font);
		double xHeight = getFloatAndCheck("xHeight", font);
		double quad = getFloatAndCheck("quad", font);

		// get optional integer attribute
		int skewChar = getOptionalInt("skewChar", font, -1);

		// get optional boolean for unicode
		int unicode = getOptionalInt("unicode", font, 0);

		// get different versions of a font
		String bold = null;
		try {
			bold = getAttrValueAndCheckIfNotNull("boldVersion", font);
		} catch (ResourceParseException e) {
		}
		String roman = null;
		try {
			roman = getAttrValueAndCheckIfNotNull("romanVersion", font);
		} catch (ResourceParseException e) {
		}
		String ss = null;
		try {
			ss = getAttrValueAndCheckIfNotNull("ssVersion", font);
		} catch (ResourceParseException e) {
		}
		String tt = null;
		try {
			tt = getAttrValueAndCheckIfNotNull("ttVersion", font);
		} catch (ResourceParseException e) {
		}
		String it = null;
		try {
			it = getAttrValueAndCheckIfNotNull("itVersion", font);
		} catch (ResourceParseException e) {
		}

		String path = name.substring(0, name.lastIndexOf("/") + 1) + fontName;

		// create FontInfo-object
		FontInfo info = new FontInfo(Font_ID.indexOf(fontId), base, path, fontName, unicode, xHeight, space,
				quad, bold, roman, ss, tt, it);

		if (skewChar != -1) // attribute set
			info.setSkewChar((char) skewChar);

		// process all "Char"-elements
		NodeList listF = font.getElementsByTagName("Char");
		for (int j = 0; j < listF.getLength(); j++)
			processCharElement(listF.item(j).castToElement(), info);

		// parsing OK, add to table
		res.add(info);

		for (int i = 0; i < res.size(); i++) {
			FontInfo fin = res.get(i);
			fin.setBoldId(Font_ID.indexOf(fin.boldVersion));
			fin.setRomanId(Font_ID.indexOf(fin.romanVersion));
			fin.setSsId(Font_ID.indexOf(fin.ssVersion));
			fin.setTtId(Font_ID.indexOf(fin.ttVersion));
			fin.setItId(Font_ID.indexOf(fin.itVersion));
		}

		parsedTextStyles = parseStyleMappings();
		return res.toArray(fi);
	}

	public FontInfo[] parseFontDescriptions(FontInfo[] fi) throws ResourceParseException {
		Element fontDescriptions = root.getElementsByTagName("FontDescriptions").item(0).castToElement();
		if (!fontDescriptions.isNull()) { // element present
			NodeList list = fontDescriptions.getElementsByTagName("Metrics");
			for (int i = 0; i < list.getLength(); i++) {
				// get required string attribute
				String include = getAttrValueAndCheckIfNotNull("include", list.item(i).castToElement());
				if (base == null) {
					fi = parseFontDescriptions(fi,
							resource.loadResource(DefaultTeXFontParser.class, include), include);
				} else {
					fi = parseFontDescriptions(fi, resource.loadResource(base, include), include);
				}
			}
		}
		return fi;
	}

	protected void parseExtraPath() throws ResourceParseException {
		Element syms = root.getElementsByTagName("TeXSymbols").item(0).castToElement();
		if (!syms.isNull()) { // element present
			// get required string attribute
			String include = getAttrValueAndCheckIfNotNull("include", syms);
			SymbolAtom.addSymbolAtom(resource.loadResource(base, include), include);
		}
		Element settings = root.getElementsByTagName("FormulaSettings").item(0).castToElement();
		if (!settings.isNull()) { // element present
			// get required string attribute
			String include = getAttrValueAndCheckIfNotNull("include", settings);
			TeXFormula.addSymbolMappings(resource.loadResource(base, include), include);
		}
	}

	private static void processCharElement(Element charElement, FontInfo info) throws ResourceParseException {
		// retrieve required integer attribute
		char ch = (char) getIntAndCheck("code", charElement);
		// retrieve optional double attributes
		double[] metrics = new double[4];
		metrics[DefaultTeXFont.WIDTH] = getOptionalFloat("width", charElement, 0);
		metrics[DefaultTeXFont.HEIGHT] = getOptionalFloat("height", charElement, 0);
		metrics[DefaultTeXFont.DEPTH] = getOptionalFloat("depth", charElement, 0);
		metrics[DefaultTeXFont.IT] = getOptionalFloat("italic", charElement, 0);
		// set metrics
		info.setMetrics(ch, metrics);

		// process children
		NodeList list = charElement.getChildNodes();
		for (int i = 0; i < list.getLength(); i++) {
			Node node = list.item(i);
			if (node.getNodeType() != Node.TEXT_NODE) {
				Element el = node.castToElement();
				Object parser = charChildParsers.get(el.getTagName());
				if (parser == null) // unknown element
					throw new XMLResourceParseException(RESOURCE_NAME
							+ ": a <Char>-element has an unknown child element '" + el.getTagName() + "'!");
				else
					// process the child element
					((CharChildParser) parser).parse(el, ch, info);
			}
		}
	}

	public static void registerFonts(boolean b) {
		shouldRegisterFonts = b;
	}

	public static Font createFont(String name) throws ResourceParseException {
		return createFont(null, name);
	}

	public static Font createFont(Object base, String name) throws ResourceParseException {
		FontAdapter fontAdapter = new FontAdapter();
		return fontAdapter.loadFont(base, name);
	}

	public Map<String, CharFont> parseSymbolMappings() throws ResourceParseException {
		Map<String, CharFont> res = new HashMap<String, CharFont>();
		Element symbolMappings = root.getElementsByTagName("SymbolMappings").item(0).castToElement();
		if (symbolMappings.isNull())
			// "SymbolMappings" is required!
			throw new XMLResourceParseException(RESOURCE_NAME, "SymbolMappings");
		else { // element present
				// iterate all mappings
			NodeList list = symbolMappings.getElementsByTagName("Mapping");
			for (int i = 0; i < list.getLength(); i++) {
				String include = getAttrValueAndCheckIfNotNull("include", list.item(i).castToElement());
				Element map;
				try {
					if (base == null) {
						map = parserAdapter.createParserAndParseFile(resource.loadResource(
								DefaultTeXFontParser.class, include));
					} else {
						map = parserAdapter.createParserAndParseFile(resource.loadResource(base, include));
					}
				} catch (Exception e) {
					throw new XMLResourceParseException("Cannot find the file " + include + "!");
				}
				NodeList listM = map.getElementsByTagName(SYMBOL_MAPPING_EL);
				for (int j = 0; j < listM.getLength(); j++) {
					Element mapping = listM.item(j).castToElement();
					// get string attribute
					String symbolName = getAttrValueAndCheckIfNotNull("name", mapping);
					// get integer attributes
					int ch = getIntAndCheck("ch", mapping);
					String fontId = getAttrValueAndCheckIfNotNull("fontId", mapping);
					// put mapping in table
					String boldFontId = null;
					try {
						boldFontId = getAttrValueAndCheckIfNotNull("boldId", mapping);
					} catch (ResourceParseException e) {
					}

					if (boldFontId == null) {
						res.put(symbolName, new CharFont((char) ch, Font_ID.indexOf(fontId)));
					} else {
						res.put(symbolName,
								new CharFont((char) ch, Font_ID.indexOf(fontId), Font_ID.indexOf(boldFontId)));
					}
				}
			}

			return res;
		}
	}

	public String[] parseDefaultTextStyleMappings() throws ResourceParseException {
		String[] res = new String[4];
		Element defaultTextStyleMappings = root.getElementsByTagName("DefaultTextStyleMapping").item(0)
				.castToElement();
		if (defaultTextStyleMappings.isNull())
			return res;
		else { // element present
				// iterate all mappings
			NodeList list = defaultTextStyleMappings.getElementsByTagName("MapStyle");
			for (int i = 0; i < list.getLength(); i++) {
				Element mapping = list.item(i).castToElement();
				// get range name and check if it's valid
				String code = getAttrValueAndCheckIfNotNull("code", mapping);
				Object codeMapping = rangeTypeMappings.get(code);
				if (codeMapping == null) // unknown range name
					throw new XMLResourceParseException(RESOURCE_NAME, "MapStyle", "code",
							"contains an unknown \"range name\" '" + code + "'!");
				// get mapped style and check if it exists
				String textStyleName = getAttrValueAndCheckIfNotNull("textStyle", mapping);
				Object styleMapping = parsedTextStyles.get(textStyleName);
				if (styleMapping == null) // unknown text style
					throw new XMLResourceParseException(RESOURCE_NAME, "MapStyle", "textStyle",
							"contains an unknown text style '" + textStyleName + "'!");
				// now check if the range is defined within the mapped text style
				CharFont[] charFonts = parsedTextStyles.get(textStyleName);
				int index = ((Integer) codeMapping).intValue();
				if (charFonts[index] == null) // range not defined
					throw new XMLResourceParseException(RESOURCE_NAME + ": the default text style mapping '"
							+ textStyleName + "' for the range '" + code
							+ "' contains no mapping for that range!");
				else
					// everything OK, put mapping in table
					res[index] = textStyleName;
			}
		}
		return res;
	}

	public Map<String, Double> parseParameters() throws ResourceParseException {
		Map<String, Double> res = new HashMap<String, Double>();
		Element parameters = root.getElementsByTagName("Parameters").item(0).castToElement();
		if (parameters.isNull())
			// "Parameters" is required!
			throw new XMLResourceParseException(RESOURCE_NAME, "Parameters");
		else { // element present
				// iterate all attributes
			NamedNodeMap list = parameters.getAttributes();
			for (int i = 0; i < list.getLength(); i++) {
				String name = (list.item(i).castToAttr()).getName();
				// set double value (if valid)
				res.put(name, new Double(getFloatAndCheck(name, parameters)));
			}
			return res;
		}
	}

	public Map<String, Number> parseGeneralSettings() throws ResourceParseException {
		Map<String, Number> res = new HashMap<String, Number>();
		// TODO: must this be 'Number' ?
		Element generalSettings = root.getElementsByTagName("GeneralSettings").item(0).castToElement();
		if (generalSettings.isNull())
			// "GeneralSettings" is required!
			throw new XMLResourceParseException(RESOURCE_NAME, "GeneralSettings");
		else { // element present
				// set required int values (if valid)
			res.put(MUFONTID_ATTR,
					Font_ID.indexOf(getAttrValueAndCheckIfNotNull(MUFONTID_ATTR, generalSettings))); // autoboxing
			res.put(SPACEFONTID_ATTR,
					Font_ID.indexOf(getAttrValueAndCheckIfNotNull(SPACEFONTID_ATTR, generalSettings))); // autoboxing
			// set required double values (if valid)
			res.put("scriptfactor", getFloatAndCheck("scriptfactor", generalSettings)); // autoboxing
			res.put("scriptscriptfactor", getFloatAndCheck("scriptscriptfactor", generalSettings)); // autoboxing

		}
		return res;
	}

	public Map<String, CharFont[]> parseTextStyleMappings() {
		return parsedTextStyles;
	}

	private Map<String, CharFont[]> parseStyleMappings() throws ResourceParseException {
		Map<String, CharFont[]> res = new HashMap<String, CharFont[]>();
		Element textStyleMappings = root.getElementsByTagName("TextStyleMappings").item(0).castToElement();
		if (textStyleMappings.isNull())
			return res;
		else { // element present
				// iterate all mappings
			NodeList list = textStyleMappings.getElementsByTagName(STYLE_MAPPING_EL);
			for (int i = 0; i < list.getLength(); i++) {
				Element mapping = list.item(i).castToElement();
				// get required string attribute
				String textStyleName = getAttrValueAndCheckIfNotNull("name", mapping);
				String boldFontId = null;
				try {
					boldFontId = getAttrValueAndCheckIfNotNull("bold", mapping);
				} catch (ResourceParseException e) {
				}

				NodeList mapRangeList = mapping.getElementsByTagName("MapRange");
				// iterate all mapping ranges
				CharFont[] charFonts = new CharFont[4];
				for (int j = 0; j < mapRangeList.getLength(); j++) {
					Element mapRange = mapRangeList.item(j).castToElement();
					// get required integer attributes
					String fontId = getAttrValueAndCheckIfNotNull("fontId", mapRange);
					int ch = getIntAndCheck("start", mapRange);
					// get required string attribute and check if it's a known range
					String code = getAttrValueAndCheckIfNotNull("code", mapRange);
					Object codeMapping = rangeTypeMappings.get(code);
					if (codeMapping == null)
						throw new XMLResourceParseException(RESOURCE_NAME, "MapRange", "code",
								"contains an unknown \"range name\" '" + code + "'!");
					else if (boldFontId == null)
						charFonts[((Integer) codeMapping).intValue()] = new CharFont((char) ch,
								Font_ID.indexOf(fontId));
					else
						charFonts[((Integer) codeMapping).intValue()] = new CharFont((char) ch,
								Font_ID.indexOf(fontId), Font_ID.indexOf(boldFontId));
				}
				res.put(textStyleName, charFonts);
			}
		}
		return res;
	}

	private static void setRangeTypeMappings() {
		rangeTypeMappings.put("numbers", DefaultTeXFont.NUMBERS); // autoboxing
		rangeTypeMappings.put("capitals", DefaultTeXFont.CAPITALS); // autoboxing
		rangeTypeMappings.put("small", DefaultTeXFont.SMALL); // autoboxing
		rangeTypeMappings.put("unicode", DefaultTeXFont.UNICODE); // autoboxing
	}

	private static String getAttrValueAndCheckIfNotNull(String attrName, Element element)
			throws ResourceParseException {
		String attrValue = element.getAttribute(attrName);
		if ("".equals(attrValue))
			throw new XMLResourceParseException(RESOURCE_NAME, element.getTagName(), attrName, null);
		return attrValue;
	}

	public static double getFloatAndCheck(String attrName, Element element) throws ResourceParseException {
		String attrValue = getAttrValueAndCheckIfNotNull(attrName, element);

		// try parsing string to double value
		double res = 0;
		try {
			res = Double.parseDouble(attrValue);
		} catch (NumberFormatException e) {
			throw new XMLResourceParseException(RESOURCE_NAME, element.getTagName(), attrName,
					"has an invalid real value!");
		}
		// parsing OK
		return res;
	}

	public static int getIntAndCheck(String attrName, Element element) throws ResourceParseException {
		String attrValue = getAttrValueAndCheckIfNotNull(attrName, element);

		// try parsing string to integer value
		int res = 0;
		try {
			res = Integer.parseInt(attrValue);
		} catch (NumberFormatException e) {
			throw new XMLResourceParseException(RESOURCE_NAME, element.getTagName(), attrName,
					"has an invalid integer value!");
		}
		// parsing OK
		return res;
	}

	public static int getOptionalInt(String attrName, Element element, int defaultValue)
			throws ResourceParseException {
		String attrValue = element.getAttribute(attrName);
		if ("".equals(attrValue)) // attribute not present
			return defaultValue;
		else {
			// try parsing string to integer value
			int res = 0;
			try {
				res = Integer.parseInt(attrValue);
			} catch (NumberFormatException e) {
				throw new XMLResourceParseException(RESOURCE_NAME, element.getTagName(), attrName,
						"has an invalid integer value!");
			}
			// parsing OK
			return res;
		}
	}

	public static double getOptionalFloat(String attrName, Element element, double defaultValue)
			throws ResourceParseException {
		String attrValue = element.getAttribute(attrName);
		if ("".equals(attrValue)) // attribute not present
			return defaultValue;
		else {
			// try parsing string to double value
			double res = 0;
			try {
				res = Double.parseDouble(attrValue);
			} catch (NumberFormatException e) {
				throw new XMLResourceParseException(RESOURCE_NAME, element.getTagName(), attrName,
						"has an invalid double value!");
			}
			// parsing OK
			return res;
		}
	}
}
