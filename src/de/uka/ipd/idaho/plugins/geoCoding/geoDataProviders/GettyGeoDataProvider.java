/*
 * Copyright (c) 2006-2008, IPD Boehm, Universitaet Karlsruhe (TH)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Universität Karlsruhe (TH) nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY UNIVERSITÄT KARLSRUHE (TH) AND CONTRIBUTORS 
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package de.uka.ipd.idaho.plugins.geoCoding.geoDataProviders;

import java.io.File;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Properties;
import java.util.TreeSet;

import de.uka.ipd.idaho.gamta.util.AnalyzerDataProviderFileBased;
import de.uka.ipd.idaho.htmlXmlUtil.Parser;
import de.uka.ipd.idaho.htmlXmlUtil.TreeNode;
import de.uka.ipd.idaho.htmlXmlUtil.accessories.IoTools;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.Html;
import de.uka.ipd.idaho.htmlXmlUtil.xPath.XPath;
import de.uka.ipd.idaho.htmlXmlUtil.xPath.types.XPathNodeSet;
import de.uka.ipd.idaho.plugins.geoCoding.GeoDataProvider;
import de.uka.ipd.idaho.plugins.geoCoding.Location;

/**
 * GeoDataProvider for Getty Thesaurus.
 * 
 * @author sautter
 */
public class GettyGeoDataProvider extends GeoDataProvider {
	
	private static final boolean DEBUG = false;
	
	private static XPath resultPath = new XPath("/HTML/BODY//FORM[./@name = 'full_display']/TR/TD/SPAN[./@class = 'page' and ./a/b]");
	private static XPath coordinatePath = new XPath("HTML/BODY/TABLE/tr/td/TABLE/TR/TD/TABLE[./TR/TD = 'Coordinates:']/TR/TD/SPAN[./I = 'decimal degrees']");
	private static XPath hierarchyPath = new XPath ("HTML/BODY/TABLE/tr/td/TABLE/TR[.//A = 'World']/TD/TABLE/TR/TD/SPAN[./A]");
	private static XPath hierarchyElementPath = new XPath ("A");
	private static XPath hierarchyTypePath = new XPath ("text()");
	
	private static XPath namePath = new XPath("/a/b");
	private static XPath detailLinkPath = new XPath("/a/@href");
//	private static XPath altNamePath = new XPath("/alternateNames");
//	private static XPath typePath = new XPath("/fclName");
//	
//	private static XPath countryPath = new XPath("/countryName");
//	private static XPath regionPath = new XPath("/adminName1");
//	private static XPath countyPath = new XPath("/adminName2");
//	
//	private static XPath longPath = new XPath("/lng");
//	private static XPath latPath = new XPath("/lat");
//	
//	private static XPath elevationPath = new XPath("/elevation");
//	
//	private static XPath populationPath = new XPath("/population");
	
	private static Parser parser = new Parser(new Html());
	
	private static String baseUrl = "http://www.getty.edu/vow/TGNServlet?english=Y&nation=&place=&find=";
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.plugins.geoCoding.GeoDataProvider#getDataSourceName()
	 */
	public String getDataSourceName() {
		return "Getty";
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.plugins.geoCoding.GeoDataProvider#isLocalDataProvider()
	 */
	protected boolean isLocalDataProvider() {
		return false;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.plugins.geoCoding.GeoDataProvider#supportsGlobalSearch()
	 */
	protected boolean supportsGlobalSearch() {
		return true;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.plugins.geoCoding.GeoDataProvider#loadLocationData(java.lang.String, java.lang.String, java.lang.String)
	 */
	public Location[] loadLocationData(String location, String nullCountry, String nullRegion) {
		ArrayList locations = new ArrayList();
		for (int p = 1; true; p++) try {
			if (DEBUG) System.out.println("  doing Getty lookup for '" + location + "' at '" + baseUrl + URLEncoder.encode(('"' + location + '"'), "UTF-8") + "&page=" + p + "'");
			TreeNode root = parser.parse(new InputStreamReader(this.dataProvider.getURL(baseUrl + URLEncoder.encode(('"' + location + '"'), "UTF-8") + "&page=" + p).openStream(), "UTF-8"));
			
			XPathNodeSet resultNodes = resultPath.evaluate(root, null);
			if (resultNodes.isEmpty())
				break;
			
			for (int r = 0; r < resultNodes.size(); r++) {
				TreeNode resultRoot = resultNodes.get(r);
				String resultType = XPath.stringValue(resultRoot).value;
				if (resultType.indexOf('(') == -1)
					continue;
				resultType = resultType.substring(resultType.indexOf('(') + "(".length()).trim();
				if (resultType.indexOf(')') == -1)
					continue;
				resultType = resultType.substring(0, resultType.indexOf(')')).trim();
				if (!returnResultTypes.contains(resultType))
					continue;
				
				String name = namePath.evaluate(resultRoot, dummyVariableBindings).asString().value;
				String detailLink = detailLinkPath.evaluate(resultRoot, dummyVariableBindings).asString().value;
				
				if (DEBUG) System.out.println("  doing detail Getty lookup for '" + name + "' at '" + detailLink + "'");
				TreeNode resultDetailPageRoot = parser.parse(new InputStreamReader(this.dataProvider.getURL(detailLink).openStream(), "UTF-8"));
				
				XPathNodeSet coordinateNodes = coordinatePath.evaluate(resultDetailPageRoot, dummyVariableBindings);
				String longitude = null;
				String latitude = null;
				for (int c = 0; c < coordinateNodes.size(); c++) {
					TreeNode coordinateNode = coordinateNodes.get(c).getChildNode(TreeNode.DATA_NODE_TYPE, 0);
					String coordinateString = coordinateNode.getNodeValue();
					coordinateString = coordinateString.replaceAll("\\&nbsp\\;", "").trim();
					if (coordinateString.startsWith("Long:"))
						longitude = coordinateString.substring("Long:".length()).trim();
					if (coordinateString.startsWith("Lat:"))
						latitude = coordinateString.substring("Lat:".length()).trim();
				}
				if ((longitude == null) || (latitude == null))
					continue;
				
				XPathNodeSet hierarchyNodes = hierarchyPath.evaluate(resultDetailPageRoot, dummyVariableBindings);
				String country = null;
				String region = null;
				String county = null;
				String type = null;
				for (int h = 0; h < hierarchyNodes.size(); h++) {
					TreeNode hierarchyNode = hierarchyNodes.get(h);
					String hierarchyType = hierarchyTypePath.evaluate(hierarchyNode, dummyVariableBindings).asString().value;
					hierarchyType = hierarchyType.substring(hierarchyType.indexOf('(') + 1);
					hierarchyType = hierarchyType.substring(0, hierarchyType.indexOf(')'));
					type = hierarchyType;
					String hierarchyElement = hierarchyElementPath.evaluate(hierarchyNode, dummyVariableBindings).asString().value;
					if ("nation".equals(hierarchyType))
						country = hierarchyElement;
					else if ("state".equals(hierarchyType))
						region = hierarchyElement;
					else if ("county".equals(hierarchyType) || "national district".equals(hierarchyType))
						county = hierarchyElement;
				}
				
				Location loc = new Location(IoTools.prepareForPlainText(name), longitude, latitude, ("" + Location.UNKNOWN_LONG_LAT_PRECISION));
				if (type != null)
					loc.attributes.setProperty(LOCATION_TYPE_ATTRIBUTE, type);
				if (country != null)
					loc.attributes.setProperty(COUNTRY_ATTRIBUTE, IoTools.prepareForPlainText(country));
				if (region != null)
					loc.attributes.setProperty(STATE_PROVINCE_ATTRIBUTE, IoTools.prepareForPlainText(region));
				if (county != null)
					loc.attributes.setProperty(COUNTY_ATTRIBUTE, IoTools.prepareForPlainText(county));
				locations.add(loc);
			}
			
			if (DEBUG) System.out.println("  found " + locations.size() + " data sets");
		}
		catch (Exception e) {
			System.out.println("GettyGeoDataProvider: " + e.getClass().getName() + " (" + e.getMessage() + ") while getting location data for '" + location + "'");
			e.printStackTrace(System.out);
		}
		return ((Location[]) locations.toArray(new Location[locations.size()]));
	}
	private static Properties dummyVariableBindings = new Properties();
	
	public static void main(String[] args) throws Exception {
		GettyGeoDataProvider ggdp = new GettyGeoDataProvider();
		
		File basePath = new File("E:/GoldenGATEv3/Plugins/AnalyzerData/GeoReferencerData/GettyData/");
		basePath.mkdirs();
		ggdp.setDataProvider(new AnalyzerDataProviderFileBased(basePath));
		
		System.out.println("GeoNamesGeoDataProvider initialized");
		
//		Location[] locations = frgdp.getLocations("Karl", false);
		Location[] locations = ggdp.getLocations("Karlsruhe", false);
		for (int l = 0; l < locations.length; l++)
			System.out.println(locations[l].toString());
	}
	
	//	list of all place types used by Getty Thesaurus, comment out the undesired ones
	private static TreeSet returnResultTypes = new TreeSet(String.CASE_INSENSITIVE_ORDER);
	static {
//		returnPlaceTypes.add("abandoned dwelling");
//		returnPlaceTypes.add("abandoned structure");
//		returnPlaceTypes.add("abandoned watercourse");
		returnResultTypes.add("abbey");
//		returnPlaceTypes.add("Aboriginal reserve");
//		returnPlaceTypes.add("abyssal plain");
//		returnPlaceTypes.add("academy");
		returnResultTypes.add("administrative center");
//		returnPlaceTypes.add("administrative division");
//		returnPlaceTypes.add("aerospace facility");
//		returnPlaceTypes.add("agricultural building");
//		returnPlaceTypes.add("agricultural center");
//		returnPlaceTypes.add("agricultural land");
//		returnPlaceTypes.add("air base");
//		returnPlaceTypes.add("airfield");
//		returnPlaceTypes.add("airport");
//		returnPlaceTypes.add("alliance");
//		returnPlaceTypes.add("American Indian reservation");
//		returnPlaceTypes.add("amusement park");
//		returnPlaceTypes.add("anabranch");
//		returnPlaceTypes.add("anchorage");
//		returnPlaceTypes.add("Ancient Greek");
//		returnPlaceTypes.add("ancient site");
//		returnPlaceTypes.add("annex");
//		returnPlaceTypes.add("aqueduct");
//		returnPlaceTypes.add("arch");
//		returnPlaceTypes.add("archaeological site");
//		returnPlaceTypes.add("Archaic (Preclassic)");
//		returnPlaceTypes.add("archdiocese");
//		returnPlaceTypes.add("archiepiscopal see");
		returnResultTypes.add("archipelago");
		returnResultTypes.add("area");
//		returnPlaceTypes.add("arena");
		returnResultTypes.add("arrondissement");
//		returnPlaceTypes.add("arroyo");
//		returnPlaceTypes.add("artists' colony");
//		returnPlaceTypes.add("association");
		returnResultTypes.add("atoll");
//		returnResultTypes.add("autonomous area");
//		returnResultTypes.add("autonomous city");
//		returnResultTypes.add("autonomous community");
//		returnResultTypes.add("autonomous district");
//		returnResultTypes.add("autonomous municipality");
//		returnResultTypes.add("autonomous province");
//		returnResultTypes.add("autonomous region");
//		returnResultTypes.add("autonomous republic");
//		returnResultTypes.add("autonomous sector");
//		returnPlaceTypes.add("Aztec");
//		returnPlaceTypes.add("bank");
//		returnPlaceTypes.add("bar");
//		returnPlaceTypes.add("base");
		returnResultTypes.add("basin");
//		returnPlaceTypes.add("battlefield park");
//		returnPlaceTypes.add("battlefield");
		returnResultTypes.add("bay");
//		returnPlaceTypes.add("bayou");
		returnResultTypes.add("beach");
//		returnPlaceTypes.add("bend");
		returnResultTypes.add("Bezirk");
		returnResultTypes.add("bight");
//		returnPlaceTypes.add("blowhole");
		returnResultTypes.add("bog");
		returnResultTypes.add("boomtown");
		returnResultTypes.add("border (boundary)");
		returnResultTypes.add("borough");
//		returnPlaceTypes.add("breakwater");
//		returnPlaceTypes.add("breeding center (animal)");
//		returnPlaceTypes.add("bridge");
//		returnPlaceTypes.add("Bronze Age");
		returnResultTypes.add("brook");
//		returnPlaceTypes.add("building");
		returnResultTypes.add("bulwark");
//		returnPlaceTypes.add("burial cave");
//		returnPlaceTypes.add("burial chamber");
//		returnPlaceTypes.add("burial site");
//		returnPlaceTypes.add("buried settlement");
//		returnPlaceTypes.add("butte");
//		returnPlaceTypes.add("Byzantine");
		returnResultTypes.add("caldera");
//		returnPlaceTypes.add("caliphate");
//		returnPlaceTypes.add("camp");
//		returnPlaceTypes.add("campus");
//		returnPlaceTypes.add("canal");
		returnResultTypes.add("canton");
		returnResultTypes.add("canyon");
		returnResultTypes.add("cape");
		returnResultTypes.add("capital");
//		returnPlaceTypes.add("castle");
//		returnPlaceTypes.add("catacombs");
//		returnPlaceTypes.add("cataract");
//		returnPlaceTypes.add("cathedral");
		returnResultTypes.add("causeway");
		returnResultTypes.add("cave dwelling");
		returnResultTypes.add("cave");
		returnResultTypes.add("caves");
//		returnPlaceTypes.add("Celtic");
//		returnPlaceTypes.add("cemetery");
//		returnPlaceTypes.add("ceremonial mound");
//		returnPlaceTypes.add("ceremonial site");
//		returnPlaceTypes.add("Chachapoya");
//		returnPlaceTypes.add("Chalcolithic (Copper Age)");
		returnResultTypes.add("channel");
//		returnPlaceTypes.add("church");
//		returnPlaceTypes.add("cirque");
//		returnPlaceTypes.add("city block");
		returnResultTypes.add("city");
		returnResultTypes.add("city-state");
//		returnPlaceTypes.add("civitas");
//		returnPlaceTypes.add("clan");
//		returnPlaceTypes.add("clearing");
		returnResultTypes.add("cliff");
		returnResultTypes.add("cliffs");
		returnResultTypes.add("cloister");
//		returnPlaceTypes.add("Clovis");
		returnResultTypes.add("coastal settlement");
		returnResultTypes.add("coastline");
//		returnPlaceTypes.add("Coclé");
//		returnPlaceTypes.add("college centers");
//		returnPlaceTypes.add("college");
//		returnPlaceTypes.add("colonial settlement");
		returnResultTypes.add("colony");
//		returnPlaceTypes.add("commandery");
//		returnPlaceTypes.add("commercial center");
		returnResultTypes.add("commonwealth");
//		returnPlaceTypes.add("commune (administrative)");
//		returnPlaceTypes.add("commune (social)");
//		returnPlaceTypes.add("communications center");
//		returnPlaceTypes.add("community");
//		returnPlaceTypes.add("complex");
//		returnPlaceTypes.add("concentration camp site");
//		returnPlaceTypes.add("concentration camp");
//		returnPlaceTypes.add("condominium");
		returnResultTypes.add("cone");
//		returnPlaceTypes.add("confederation");
//		returnPlaceTypes.add("confluence");
//		returnPlaceTypes.add("constitutional monarchy");
//		returnPlaceTypes.add("continent");
//		returnPlaceTypes.add("continents");
//		returnPlaceTypes.add("controlled region");
//		returnPlaceTypes.add("convent");
//		returnPlaceTypes.add("corporate headquarters");
//		returnPlaceTypes.add("correctional institution");
//		returnPlaceTypes.add("country house");
		returnResultTypes.add("country");
		returnResultTypes.add("countship");
		returnResultTypes.add("county seat");
		returnResultTypes.add("county");
//		returnPlaceTypes.add("courthouse");
		returnResultTypes.add("cove");
//		returnPlaceTypes.add("cow town");
//		returnPlaceTypes.add("craftsman settlement");
		returnResultTypes.add("crater lake");
		returnResultTypes.add("crater");
		returnResultTypes.add("creek");
//		returnPlaceTypes.add("crossing");
//		returnPlaceTypes.add("cultural center");
//		returnPlaceTypes.add("cultural group");
//		returnPlaceTypes.add("current");
//		returnPlaceTypes.add("deep");
//		returnPlaceTypes.add("defense installation");
//		returnPlaceTypes.add("defile");
		returnResultTypes.add("delta");
		returnResultTypes.add("department capital");
		returnResultTypes.add("department");
//		returnPlaceTypes.add("dependency");
//		returnPlaceTypes.add("dependent political entity");
//		returnPlaceTypes.add("dependent state");
//		returnPlaceTypes.add("depot");
		returnResultTypes.add("depression");
		returnResultTypes.add("desert");
//		returnPlaceTypes.add("deserted settlement");
//		returnPlaceTypes.add("despotate");
//		returnPlaceTypes.add("dictatorship");
		returnResultTypes.add("dike");
//		returnPlaceTypes.add("diocese");
//		returnPlaceTypes.add("diplomatic building(s)");
//		returnPlaceTypes.add("disputed territory");
//		returnResultTypes.add("distributary (stream)");
		returnResultTypes.add("district capital");
		returnResultTypes.add("ditch");
		returnResultTypes.add("divide");
//		returnPlaceTypes.add("dock");
//		returnPlaceTypes.add("dockyard");
		returnResultTypes.add("dominion");
//		returnPlaceTypes.add("drainage basin");
//		returnPlaceTypes.add("dry lake");
//		returnPlaceTypes.add("ducal residence");
		returnResultTypes.add("duchy");
		returnResultTypes.add("dunes");
//		returnPlaceTypes.add("dwarf planet");
//		returnPlaceTypes.add("dynasty");
//		returnPlaceTypes.add("Early Iron Age");
//		returnPlaceTypes.add("educational center");
//		returnPlaceTypes.add("electorate");
		returnResultTypes.add("emirate");
		returnResultTypes.add("empire");
		returnResultTypes.add("enclave");
//		returnPlaceTypes.add("Eneolithic");
//		returnPlaceTypes.add("entertainment center");
//		returnPlaceTypes.add("eparchy");
//		returnPlaceTypes.add("ephemeral community");
//		returnPlaceTypes.add("episcopal see");
//		returnPlaceTypes.add("Episcopal");
		returnResultTypes.add("escarpment");
//		returnPlaceTypes.add("estate");
		returnResultTypes.add("estuary");
//		returnPlaceTypes.add("ethnic group");
//		returnPlaceTypes.add("exhibition building");
//		returnPlaceTypes.add("external territory");
//		returnPlaceTypes.add("factory center");
//		returnPlaceTypes.add("family");
		returnResultTypes.add("farm");
		returnResultTypes.add("fault");
//		returnResultTypes.add("federal capital territory");
//		returnResultTypes.add("federal territory");
		returnResultTypes.add("federation");
		returnResultTypes.add("fief");
		returnResultTypes.add("field");
//		returnPlaceTypes.add("financial center");
		returnResultTypes.add("fiord");
//		returnPlaceTypes.add("fire station");
//		returnPlaceTypes.add("First Intermediate period");
//		returnResultTypes.add("first level subdivision");
//		returnPlaceTypes.add("fishing community");
//		returnPlaceTypes.add("fishing spot");
//		returnPlaceTypes.add("fish-processing center");
//		returnPlaceTypes.add("fissure");
//		returnPlaceTypes.add("flat");
//		returnPlaceTypes.add("flood plain");
//		returnPlaceTypes.add("fluvial island");
		returnResultTypes.add("ford");
		returnResultTypes.add("forest reserve");
		returnResultTypes.add("forest station");
		returnResultTypes.add("forest");
//		returnPlaceTypes.add("former administrative division");
//		returnPlaceTypes.add("former body of water");
//		returnPlaceTypes.add("former community");
//		returnPlaceTypes.add("former group of nations/states/cities");
//		returnPlaceTypes.add("former island");
//		returnPlaceTypes.add("former landmass");
//		returnPlaceTypes.add("former nation/state/empire");
//		returnPlaceTypes.add("former physical feature");
//		returnPlaceTypes.add("former reservoir");
//		returnPlaceTypes.add("former structure");
//		returnPlaceTypes.add("fort");
//		returnPlaceTypes.add("fortification");
//		returnPlaceTypes.add("fortified settlement");
//		returnPlaceTypes.add("fortress");
//		returnPlaceTypes.add("forum");
//		returnResultTypes.add("fourth level subdivision");
//		returnPlaceTypes.add("frazione");
		returnResultTypes.add("free port");
//		returnPlaceTypes.add("frontier settlement");
//		returnPlaceTypes.add("frontier");
//		returnPlaceTypes.add("funerary building");
//		returnPlaceTypes.add("fur station");
//		returnPlaceTypes.add("Gallo-Roman");
//		returnPlaceTypes.add("gap");
		returnResultTypes.add("garden suburb");
		returnResultTypes.add("garden");
//		returnPlaceTypes.add("gateway");
//		returnPlaceTypes.add("general region");
//		returnPlaceTypes.add("geological formation");
//		returnPlaceTypes.add("geyser");
//		returnPlaceTypes.add("ghost town");
		returnResultTypes.add("glacier");
//		returnPlaceTypes.add("gold town");
		returnResultTypes.add("gorge");
//		returnPlaceTypes.add("government office building");
//		returnPlaceTypes.add("governmental installation");
		returnResultTypes.add("governorate");
		returnResultTypes.add("grand duchy");
		returnResultTypes.add("grassland");
//		returnPlaceTypes.add("grave");
//		returnPlaceTypes.add("grazing area");
//		returnPlaceTypes.add("group of nations/states/cities");
		returnResultTypes.add("grove");
		returnResultTypes.add("gulf");
		returnResultTypes.add("gully");
//		returnPlaceTypes.add("Hallstatt");
		returnResultTypes.add("hamlet");
//		returnPlaceTypes.add("Han (Chinese)");
		returnResultTypes.add("harbor");
//		returnPlaceTypes.add("headland");
//		returnPlaceTypes.add("headwater");
//		returnPlaceTypes.add("health facility");
		returnResultTypes.add("heath");
		returnResultTypes.add("hermitage");
//		returnPlaceTypes.add("hierarchy root");
		returnResultTypes.add("highland");
//		returnPlaceTypes.add("highway");
//		returnPlaceTypes.add("hill settlement");
		returnResultTypes.add("hill");
		returnResultTypes.add("hills");
//		returnPlaceTypes.add("historic building");
//		returnPlaceTypes.add("historic site");
//		returnPlaceTypes.add("historic structure");
//		returnPlaceTypes.add("historical cultural group");
//		returnPlaceTypes.add("historical park");
//		returnPlaceTypes.add("historical region");
//		returnPlaceTypes.add("holy precinct");
//		returnPlaceTypes.add("hospital");
//		returnPlaceTypes.add("house");
//		returnPlaceTypes.add("housing development");
//		returnPlaceTypes.add("hunting base");
//		returnPlaceTypes.add("icecap");
		returnResultTypes.add("imperial city");
//		returnPlaceTypes.add("inactive mine");
//		returnPlaceTypes.add("Inca");
//		returnPlaceTypes.add("independent city");
//		returnPlaceTypes.add("independent nation");
//		returnPlaceTypes.add("independent political entity");
//		returnPlaceTypes.add("independent sovereign nation");
//		returnPlaceTypes.add("Indian");
//		returnPlaceTypes.add("Indigenous Protected Area");
//		returnPlaceTypes.add("industrial center");
//		returnPlaceTypes.add("industrial complex");
		returnResultTypes.add("inhabited place");
		returnResultTypes.add("inhabited region");
		returnResultTypes.add("inland port");
		returnResultTypes.add("inlet");
//		returnPlaceTypes.add("institutional building");
//		returnPlaceTypes.add("intermittent body of water");
//		returnPlaceTypes.add("intermittent lake");
//		returnPlaceTypes.add("intermittent watercourse");
//		returnPlaceTypes.add("intermittent wetland");
//		returnPlaceTypes.add("Iron Age");
//		returnPlaceTypes.add("irrigation center");
		returnResultTypes.add("island group");
		returnResultTypes.add("island nation");
		returnResultTypes.add("island");
		returnResultTypes.add("islands");
		returnResultTypes.add("islet");
		returnResultTypes.add("islets");
		returnResultTypes.add("isthmus");
//		returnPlaceTypes.add("judicial center");
		returnResultTypes.add("jungle");
		returnResultTypes.add("khanate");
//		returnPlaceTypes.add("kibbutz");
		returnResultTypes.add("kingdom");
		returnResultTypes.add("lagoon");
		returnResultTypes.add("lake channel");
		returnResultTypes.add("lake");
		returnResultTypes.add("lakes");
//		returnPlaceTypes.add("land bridge");
//		returnPlaceTypes.add("land grant");
		returnResultTypes.add("landgraviate");
		returnResultTypes.add("landing");
		returnResultTypes.add("land-tied island");
//		returnPlaceTypes.add("lava flow");
//		returnPlaceTypes.add("league");
//		returnPlaceTypes.add("legislative center");
//		returnPlaceTypes.add("leper colony");
//		returnPlaceTypes.add("levee");
//		returnPlaceTypes.add("lighthouse");
//		returnPlaceTypes.add("livestock center");
//		returnPlaceTypes.add("local council");
//		returnPlaceTypes.add("local district");
//		returnPlaceTypes.add("local region");
//		returnPlaceTypes.add("locale");
//		returnPlaceTypes.add("locality");
//		returnPlaceTypes.add("lock");
//		returnPlaceTypes.add("lordship");
//		returnPlaceTypes.add("lost area");
//		returnPlaceTypes.add("lost settlement");
//		returnPlaceTypes.add("lost watercourse");
//		returnPlaceTypes.add("lumber camp");
		returnResultTypes.add("manor");
//		returnPlaceTypes.add("mansion");
//		returnPlaceTypes.add("manufacturing center");
//		returnPlaceTypes.add("march");
		returnResultTypes.add("margraviate");
		returnResultTypes.add("marina");
		returnResultTypes.add("marine channel");
//		returnPlaceTypes.add("marine park");
//		returnPlaceTypes.add("marine sanctuary");
//		returnPlaceTypes.add("market center");
		returnResultTypes.add("marsh");
		returnResultTypes.add("massif");
//		returnPlaceTypes.add("mausoleum");
//		returnPlaceTypes.add("Maya (Mayan)");
		returnResultTypes.add("meadow");
		returnResultTypes.add("meander");
//		returnPlaceTypes.add("medical center");
//		returnPlaceTypes.add("Medieval");
//		returnPlaceTypes.add("megalithic site");
//		returnPlaceTypes.add("memorial");
//		returnPlaceTypes.add("mesa");
//		returnPlaceTypes.add("metallurgical center");
//		returnPlaceTypes.add("meteorological station");
		returnResultTypes.add("metropolis");
		returnResultTypes.add("metropolitan area");
//		returnPlaceTypes.add("military center");
//		returnPlaceTypes.add("military installation");
//		returnPlaceTypes.add("military park");
//		returnPlaceTypes.add("military regime");
//		returnPlaceTypes.add("mill center");
//		returnPlaceTypes.add("mine");
//		returnPlaceTypes.add("mines");
//		returnPlaceTypes.add("mining center");
//		returnPlaceTypes.add("miscellaneous");
//		returnPlaceTypes.add("mission");
//		returnPlaceTypes.add("moat(s)");
		returnResultTypes.add("mole");
		returnResultTypes.add("monarchy");
		returnResultTypes.add("monastery");
//		returnPlaceTypes.add("monastic center");
//		returnPlaceTypes.add("monument");
//		returnPlaceTypes.add("moon");
//		returnPlaceTypes.add("moor(s)");
//		returnPlaceTypes.add("mosque");
//		returnPlaceTypes.add("mound(s)");
		returnResultTypes.add("mountain range");
		returnResultTypes.add("mountain system");
		returnResultTypes.add("mountain");
		returnResultTypes.add("mountains");
		returnResultTypes.add("municipality");
		returnResultTypes.add("municipium");
//		returnPlaceTypes.add("museum");
		returnResultTypes.add("nation");
//		returnResultTypes.add("national capital");
//		returnPlaceTypes.add("national cemetery");
//		returnResultTypes.add("national district");
//		returnResultTypes.add("national division");
		returnResultTypes.add("national forest");
//		returnPlaceTypes.add("national monument");
		returnResultTypes.add("national park");
//		returnResultTypes.add("national region");
//		returnResultTypes.add("national subdivision");
//		returnPlaceTypes.add("Native American");
//		returnPlaceTypes.add("native peoples reservation");
//		returnPlaceTypes.add("natural arch");
//		returnPlaceTypes.add("natural pillar");
//		returnPlaceTypes.add("nature reserve");
//		returnPlaceTypes.add("naval base");
		returnResultTypes.add("navigation channel");
//		returnPlaceTypes.add("necropolis");
//		returnPlaceTypes.add("neighborhood");
//		returnPlaceTypes.add("Neolithic");
//		returnPlaceTypes.add("neutral zone");
//		returnPlaceTypes.add("noble seat");
//		returnPlaceTypes.add("Norse");
//		returnPlaceTypes.add("nunatak");
		returnResultTypes.add("oasis settlement");
		returnResultTypes.add("oblast");
//		returnPlaceTypes.add("observatory");
//		returnPlaceTypes.add("occupied territory");
		returnResultTypes.add("ocean");
//		returnPlaceTypes.add("office building");
//		returnPlaceTypes.add("official residence");
//		returnPlaceTypes.add("oil field site");
//		returnPlaceTypes.add("oil field");
//		returnPlaceTypes.add("oil terminal");
//		returnPlaceTypes.add("oligarchy");
//		returnPlaceTypes.add("Olmec");
		returnResultTypes.add("oppidum");
//		returnPlaceTypes.add("orchard");
//		returnPlaceTypes.add("organization");
//		returnPlaceTypes.add("Ortsteil");
//		returnPlaceTypes.add("Ottoman");
//		returnPlaceTypes.add("overseas department");
//		returnPlaceTypes.add("overseas province");
//		returnPlaceTypes.add("overseas territory");
//		returnPlaceTypes.add("Paleolithic");
//		returnPlaceTypes.add("paleontological site");
//		returnPlaceTypes.add("pan");
//		returnPlaceTypes.add("papal residence");
//		returnPlaceTypes.add("parish (ecclesiastical)");
//		returnPlaceTypes.add("parish (political)");
//		returnPlaceTypes.add("park");
//		returnPlaceTypes.add("parking garage");
//		returnPlaceTypes.add("parkway");
//		returnPlaceTypes.add("part of inhabited place");
		returnResultTypes.add("pass");
		returnResultTypes.add("passage");
//		returnPlaceTypes.add("patriarchal see");
		returnResultTypes.add("patriarchy");
		returnResultTypes.add("peak");
//		returnPlaceTypes.add("pedestrian mall");
//		returnPlaceTypes.add("penal settlement");
		returnResultTypes.add("peninsula");
//		returnPlaceTypes.add("Perigordian");
//		returnPlaceTypes.add("petroleum refining center");
//		returnPlaceTypes.add("Phoenician");
//		returnPlaceTypes.add("physical feature");
		returnResultTypes.add("piazza");
		returnResultTypes.add("pier");
//		returnResultTypes.add("pilgrimage center");
		returnResultTypes.add("plain");
//		returnPlaceTypes.add("planet");
//		returnPlaceTypes.add("planetary body");
//		returnPlaceTypes.add("planetary system");
//		returnPlaceTypes.add("plantation");
		returnResultTypes.add("plateau");
//		returnResultTypes.add("plaza (administrative)");
		returnResultTypes.add("point");
		returnResultTypes.add("polder");
		returnResultTypes.add("poleis");
//		returnPlaceTypes.add("political center");
//		returnPlaceTypes.add("political entity");
		returnResultTypes.add("pond");
//		returnPlaceTypes.add("pool");
//		returnPlaceTypes.add("port of entry");
		returnResultTypes.add("port");
//		returnPlaceTypes.add("possession");
//		returnPlaceTypes.add("post office");
//		returnPlaceTypes.add("power plant");
//		returnPlaceTypes.add("power production center");
//		returnPlaceTypes.add("Preclassic");
//		returnPlaceTypes.add("pre-colonial");
//		returnPlaceTypes.add("Pre-Columbian");
		returnResultTypes.add("prefecture");
//		returnPlaceTypes.add("prehistoric site");
//		returnPlaceTypes.add("preserve");
//		returnPlaceTypes.add("presidio");
//		returnResultTypes.add("primary political unit");
//		returnPlaceTypes.add("prince-bishopric");
		returnResultTypes.add("principality");
//		returnPlaceTypes.add("priory");
//		returnPlaceTypes.add("prison center");
//		returnPlaceTypes.add("prison");
//		returnPlaceTypes.add("prisoner of war camp");
		returnResultTypes.add("promontory");
//		returnPlaceTypes.add("protected area");
//		returnPlaceTypes.add("protected state");
		returnResultTypes.add("protectorate");
		returnResultTypes.add("province");
//		returnPlaceTypes.add("provincial capital");
//		returnPlaceTypes.add("provincial park");
//		returnPlaceTypes.add("publishing center");
		returnResultTypes.add("pueblo");
//		returnPlaceTypes.add("pyramid");
//		returnPlaceTypes.add("quarrying center");
		returnResultTypes.add("quarter");
		returnResultTypes.add("quartiere");
		returnResultTypes.add("quay");
//		returnPlaceTypes.add("railroad camp");
//		returnPlaceTypes.add("railroad station");
//		returnPlaceTypes.add("railroad");
		returnResultTypes.add("rain forest");
//		returnPlaceTypes.add("ranch");
		returnResultTypes.add("rapids");
		returnResultTypes.add("ravine");
		returnResultTypes.add("reach");
//		returnPlaceTypes.add("recreation area");
		returnResultTypes.add("reef");
//		returnPlaceTypes.add("refugee camp");
//		returnPlaceTypes.add("refugee center");
//		returnPlaceTypes.add("region (administrative division)");
		returnResultTypes.add("region (geographic)");
		returnResultTypes.add("regional capital");
		returnResultTypes.add("regional center");
		returnResultTypes.add("regional division");
//		returnPlaceTypes.add("religious building");
//		returnPlaceTypes.add("religious center");
//		returnPlaceTypes.add("religious community");
		returnResultTypes.add("republic");
//		returnPlaceTypes.add("reservoir");
//		returnPlaceTypes.add("residential center");
//		returnPlaceTypes.add("resort center");
//		returnPlaceTypes.add("resort");
		returnResultTypes.add("ridge");
//		returnPlaceTypes.add("rione");
		returnResultTypes.add("river mouth(s)");
//		returnPlaceTypes.add("river port");
//		returnPlaceTypes.add("river settlement");
		returnResultTypes.add("river");
//		returnPlaceTypes.add("road");
//		returnPlaceTypes.add("roadside rest area");
//		returnPlaceTypes.add("rock shelter");
		returnResultTypes.add("rock");
//		returnPlaceTypes.add("Roman");
//		returnPlaceTypes.add("Romano-British");
		returnResultTypes.add("route");
//		returnPlaceTypes.add("royal residence");
//		returnPlaceTypes.add("ruined settlement");
//		returnPlaceTypes.add("ruins");
		returnResultTypes.add("run");
//		returnResultTypes.add("rural community");
//		returnResultTypes.add("rural district");
//		returnPlaceTypes.add("sacred site");
		returnResultTypes.add("saddle");
//		returnPlaceTypes.add("salt area");
//		returnPlaceTypes.add("salt flat");
		returnResultTypes.add("salt lake");
		returnResultTypes.add("salt marsh");
//		returnPlaceTypes.add("sanctuary");
		returnResultTypes.add("sand area");
		returnResultTypes.add("sand bar");
		returnResultTypes.add("sandy desert");
//		returnPlaceTypes.add("sanitarium");
//		returnPlaceTypes.add("satellite");
//		returnPlaceTypes.add("satrapy");
//		returnPlaceTypes.add("school");
//		returnPlaceTypes.add("scientific center");
//		returnPlaceTypes.add("scientific facility");
//		returnPlaceTypes.add("scientific research base");
		returnResultTypes.add("scrubland");
		returnResultTypes.add("sea");
//		returnPlaceTypes.add("seaport");
		returnResultTypes.add("seasonally inhabited place");
//		returnResultTypes.add("second level subdivision");
		returnResultTypes.add("section of watercourse");
//		returnPlaceTypes.add("semi-independent political entity");
//		returnPlaceTypes.add("senatorial district");
//		returnPlaceTypes.add("sestiere");
//		returnPlaceTypes.add("sewage treatment plant");
//		returnPlaceTypes.add("Shang");
		returnResultTypes.add("sheikhdom");
//		returnPlaceTypes.add("shipbuilding center");
//		returnPlaceTypes.add("shipping center");
//		returnPlaceTypes.add("shipyard");
		returnResultTypes.add("shire");
		returnResultTypes.add("shoal");
//		returnPlaceTypes.add("shopping center");
		returnResultTypes.add("shore");
//		returnPlaceTypes.add("shrine");
		returnResultTypes.add("sinkhole");
		returnResultTypes.add("site");
//		returnPlaceTypes.add("skid row");
		returnResultTypes.add("slope");
		returnResultTypes.add("slough");
		returnResultTypes.add("sluice");
		returnResultTypes.add("snowfield");
//		returnPlaceTypes.add("social group");
		returnResultTypes.add("sound");
//		returnPlaceTypes.add("spa center");
//		returnPlaceTypes.add("special area");
//		returnPlaceTypes.add("special city");
//		returnPlaceTypes.add("special municipality");
//		returnPlaceTypes.add("special territory");
		returnResultTypes.add("spit");
//		returnPlaceTypes.add("sporting center");
		returnResultTypes.add("spring");
		returnResultTypes.add("spur");
		returnResultTypes.add("square");
//		returnPlaceTypes.add("stage station");
//		returnPlaceTypes.add("star");
		returnResultTypes.add("state capital");
		returnResultTypes.add("state forest");
		returnResultTypes.add("state park");
		returnResultTypes.add("state");
//		returnPlaceTypes.add("Stone Age");
		returnResultTypes.add("strait");
		returnResultTypes.add("stream channel");
		returnResultTypes.add("stream");
		returnResultTypes.add("streams");
//		returnPlaceTypes.add("street address");
//		returnPlaceTypes.add("street");
//		returnPlaceTypes.add("structure");
//		returnPlaceTypes.add("subcontinent");
//		returnPlaceTypes.add("subdivision");
//		returnPlaceTypes.add("submerged feature");
		returnResultTypes.add("subregional capital");
//		returnPlaceTypes.add("suburb");
//		returnPlaceTypes.add("subway station");
		returnResultTypes.add("sultanate");
//		returnPlaceTypes.add("sun");
//		returnPlaceTypes.add("suzerainty");
		returnResultTypes.add("swamp");
//		returnPlaceTypes.add("synagogue");
//		returnPlaceTypes.add("temple");
		returnResultTypes.add("territory");
//		returnPlaceTypes.add("textile center");
//		returnPlaceTypes.add("theater");
//		returnResultTypes.add("third level subdivision");
//		returnPlaceTypes.add("Thracian");
//		returnPlaceTypes.add("tidal watercourse");
//		returnPlaceTypes.add("timber center");
//		returnPlaceTypes.add("tomb(s)");
//		returnPlaceTypes.add("tourist center");
//		returnPlaceTypes.add("tower");
		returnResultTypes.add("town");
		returnResultTypes.add("township");
//		returnPlaceTypes.add("trade center");
		returnResultTypes.add("trail");
//		returnPlaceTypes.add("transit terminal");
//		returnPlaceTypes.add("transport point");
//		returnPlaceTypes.add("transportation center");
//		returnPlaceTypes.add("treaty port");
		returnResultTypes.add("trees");
//		returnPlaceTypes.add("triangulation station");
//		returnPlaceTypes.add("tribal area");
//		returnPlaceTypes.add("tribe");
//		returnPlaceTypes.add("tributary (political)");
//		returnPlaceTypes.add("trust territory");
//		returnPlaceTypes.add("tunnel");
		returnResultTypes.add("undersea bank");
		returnResultTypes.add("undersea basin");
		returnResultTypes.add("undersea hole");
		returnResultTypes.add("undersea ledge");
		returnResultTypes.add("undersea slope");
		returnResultTypes.add("undersea trough");
		returnResultTypes.add("underwater site");
//		returnPlaceTypes.add("unincorporated area");
//		returnPlaceTypes.add("unincorporated territory");
//		returnPlaceTypes.add("union territory");
//		returnPlaceTypes.add("union");
//		returnPlaceTypes.add("unitary authority");
//		returnPlaceTypes.add("university center");
//		returnPlaceTypes.add("university");
		returnResultTypes.add("upland");
//		returnPlaceTypes.add("Upper Perigordian");
//		returnPlaceTypes.add("urban center");
//		returnPlaceTypes.add("urban district");
//		returnPlaceTypes.add("urban park");
//		returnPlaceTypes.add("urban prefecture");
//		returnPlaceTypes.add("urban subdivision");
//		returnPlaceTypes.add("vassal state");
//		returnPlaceTypes.add("viceroyalty");
//		returnPlaceTypes.add("villa");
		returnResultTypes.add("village");
		returnResultTypes.add("villeneuve");
//		returnPlaceTypes.add("vineyard");
//		returnPlaceTypes.add("viniculture center");
		returnResultTypes.add("voivodship");
		returnResultTypes.add("volcanic landform");
		returnResultTypes.add("volcano");
//		returnPlaceTypes.add("wall");
		returnResultTypes.add("watercourse");
		returnResultTypes.add("waterfall");
		returnResultTypes.add("waterhole");
		returnResultTypes.add("weapons production center");
		returnResultTypes.add("well");
		returnResultTypes.add("wetland");
//		returnPlaceTypes.add("whaling station");
//		returnPlaceTypes.add("wharf");
//		returnPlaceTypes.add("whirlpool");
//		returnPlaceTypes.add("wilderness area");
		returnResultTypes.add("wildlife refuge");
		returnResultTypes.add("woods");
//		returnPlaceTypes.add("World Heritage Site");
//		returnPlaceTypes.add("zoo");
	}
}