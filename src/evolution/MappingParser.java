/*******************************************************************************
 * Copyright 2016 by the Department of Computer Science (University of Genova and University of Oxford)
 * 
 *    This file is part of LogMapC an extension of LogMap matcher for conservativity principle.
 * 
 *    LogMapC is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Lesser General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 * 
 *    LogMapC is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Lesser General Public License for more details.
 * 
 *    You should have received a copy of the GNU Lesser General Public License
 *    along with LogMapC.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package evolution;

import java.io.File;
import java.io.IOException;
import java.util.GregorianCalendar;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import com.hp.hpl.jena.graph.query.Mapping;

public class MappingParser extends DefaultHandler
{
  private		 String	mappingFileLocation = null;
  public		 ImportMapping	  mappingToImport = null;
  private		 Stack<String>		  stack = null;
  private		 String[] currentAttributeValue = null;
  private 		 ImportCorrespondence currentCorrespondence = null;
  private		 String[] currentObject = null;

  // ---- main ----

  public static void main( String[] argv ) {
	  if( argv.length != 1 ) {
		  System.err.println( "Usage: java ExampleSaxEcho MyXmlFile.xml" );
		  System.exit( 1 );
	  }
	  try {
		  // Use an instance of ourselves as the SAX event handler
		  DefaultHandler handler = new MappingParser(argv[0]);
		  // Parse the input with the default (non-validating) parser
		  SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
		  saxParser.parse( new File( argv[0] ), handler );
		  
		  System.out.println(((MappingParser)handler).mappingToImport);
		  
		  System.exit( 0 );
	  } catch( Throwable t ) {
		  t.printStackTrace();
		  System.exit( 2 );
	  }
  }
  
  public MappingParser(String mappingFile) {
	  this.mappingFileLocation = mappingFile;
  }
  
  public void importMapping() {
	  try {
		long start, duration;
		start = System.currentTimeMillis();
		System.out.println("Importing Mapping of File: "+this.mappingFileLocation);
		System.out.print("Parsing the mapping ...");
		SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
		saxParser.parse( new File( this.mappingFileLocation ), this );
		duration = (System.currentTimeMillis()-start);
		System.out.println("  Done ! ("+duration+" ms)");
//		if (this.mappingToImport.isExternalImport) {
//			start = System.currentTimeMillis();
//			System.out.print("Wrapping external mapping ...");
//			doExternalImport();
//			duration = (System.currentTimeMillis()-start);
//			System.out.println("  Done ! ("+duration+" ms)");
//		}
		start = System.currentTimeMillis();
		System.out.print("Importing the mapping ...");
		//api = APIFactory.getInstance().getMappingVersionImportAPI();
		//api.importMapping(this.mappingToImport);
		duration = (System.currentTimeMillis()-start);
		System.out.println("  Done ! ("+duration+" ms)");
	} catch (ParserConfigurationException e) {
		throw new RuntimeException(e.getMessage());
	} catch (SAXException e) {
		throw new RuntimeException(e.getMessage());
	} catch (IOException e) {
		throw new RuntimeException(e.getMessage());
	}
  }

//  public void doExternalImport() {
//	  String accessClass = this.mappingToImport.accessClass;
//	  try {
//			Class<?> resultClass = Class.forName(accessClass);
//			MappingWrapper result = (MappingWrapper)resultClass.newInstance();
//			result.getCorrespondences(this.mappingToImport);
//		} catch (ClassNotFoundException e) {
//			throw new RuntimeException(e.getMessage());
//		} catch (InstantiationException e) {
//			throw new RuntimeException(e.getMessage());
//		} catch (IllegalAccessException e) {
//			throw new RuntimeException(e.getMessage());
//		}
//  }
  
  public void startDocument() throws SAXException {
    this.stack = new Stack<String>();
  }

  public void endDocument() throws SAXException {
  }

  public void startElement( String namespaceURI,String localName,String qName,Attributes attrs ) throws SAXException {
	  if (qName.equals("mapping")) {
    	this.mappingToImport = new ImportMapping();
    	if (attrs!=null) {
    		for (int i=0;i<attrs.getLength();i++) {
    			String attName = attrs.getQName(i);
    			if (attName.equalsIgnoreCase("baseName")) {
    				this.mappingToImport.baseName = attrs.getValue(i).trim();
    			} else if (attName.equalsIgnoreCase("versionName")) {
    				this.mappingToImport.versionName = attrs.getValue(i).trim();
    			} else if (attName.equalsIgnoreCase("mapping_class")) {
    				this.mappingToImport.mappingClass = attrs.getValue(i).trim();
    			} else if (attName.equalsIgnoreCase("mapping_type")) {
    				this.mappingToImport.mappingType = attrs.getValue(i).trim();
    			} else if (attName.equalsIgnoreCase("mapping_method")) {
    				this.mappingToImport.mappingMethod = attrs.getValue(i).trim();
        		} else if (attName.equalsIgnoreCase("mapping_tool")) {
        			this.mappingToImport.mappingTool = attrs.getValue(i).trim();
    			} else if (attName.equalsIgnoreCase("timestamp")) {
    				String[] dateParts = attrs.getValue(i).trim().split("-");
    				this.mappingToImport.mappingTimestamp = new GregorianCalendar(Integer.parseInt(dateParts[0]),Integer.parseInt(dateParts[1])-1,Integer.parseInt(dateParts[2]));
    			} else if (attName.equalsIgnoreCase("is_instance_map")) {
    				String attValue = attrs.getValue(i);
    				if (attValue.equalsIgnoreCase("yes")||attValue.equalsIgnoreCase("true")) {
    					this.mappingToImport.isInstanceMap = true;
    				} else {
    					this.mappingToImport.isInstanceMap = false;
    				}
    			}
    		}
    	}
    	
    } else if (qName.equals("metadata")) {
    	if (attrs!=null) {
    		for (int i=0;i<attrs.getLength();i++) {
    			String attName = attrs.getQName(i);    			
    			if (attName.equalsIgnoreCase("minConfidence")) {
    				this.mappingToImport.minConfidence = Float.parseFloat(attrs.getValue(i).trim());
    			} else if (attName.equalsIgnoreCase("minSupport")) {
    				this.mappingToImport.minSupport = Integer.parseInt(attrs.getValue(i).trim());
    			}
    		}
    	}
    } else if (qName.equals("domain_sources")) {
    	
    } else if (qName.equals("range_sources")) {
    	
    } else if (qName.equals("instance_sources")) {
    
    } else if (qName.equals("source")) {
//    	ImportSource sv = new ImportSource();
//    	if (attrs!=null) {
//    		for (int i=0;i<attrs.getLength();i++) {
//    			String attName = attrs.getQName(i);
//    			if (attName.equalsIgnoreCase("objecttype")) {
//    				sv.objectType = attrs.getValue(i).trim();
//    			} else if (attName.equalsIgnoreCase("name")) {
//    				sv.sourceName = attrs.getValue(i).trim();
//    			} else if (attName.equalsIgnoreCase("timestamp")) {
//    				String[] dateParts = attrs.getValue(i).trim().split("-");
//    				sv.timestamp = new GregorianCalendar(Integer.parseInt(dateParts[0]),Integer.parseInt(dateParts[1])-1,Integer.parseInt(dateParts[2]));
//    			} else if (attName.equalsIgnoreCase("version")) {
//    				sv.version = attrs.getValue(i);
//    			} else if (attName.equalsIgnoreCase("is_ontology")) {
//    				String attValue = attrs.getValue(i);
//    				if (attValue.equalsIgnoreCase("yes")||attValue.equalsIgnoreCase("true")) {
//    					sv.isOntology = true;
//    				} else {
//    					sv.isOntology = false;
//    				}
//    			}
//    		}
//    	}
//    	String lastElement = stack.pop();
//    	String secondLastElement = stack.peek();
//    	stack.push(lastElement);
//    	if (lastElement.equals("domain_sources")) {
//    		if (secondLastElement.equals("instance_sources")) {
//    			this.mappingToImport.addInstanceDomainSource(sv);
//    		} else {
//    			this.mappingToImport.addDomainSource(sv);
//    		}
//    	} else if (lastElement.equals("range_sources")) {
//    		if (secondLastElement.equals("instance_sources")) {
//    			this.mappingToImport.addInstanceRangeSource(sv);
//    		} else {
//    			this.mappingToImport.addRangeSource(sv);
//    		}
//    	}
    } else if (qName.equals("correspondences")) {
    	//
    } else if (qName.equals("correspondence")) {
    	this.currentCorrespondence = new ImportCorrespondence();
    	if (attrs!=null) {
    		for (int i=0;i<attrs.getLength();i++) {
    			String attName = attrs.getQName(i);
    			String attValue = attrs.getValue(i);
    			if (attName.equalsIgnoreCase("confidence")) {
    				this.currentCorrespondence.confidence = Float.parseFloat(attValue);
    			} else if (attName.equalsIgnoreCase("support")) {
    				this.currentCorrespondence.support = Integer.parseInt(attValue);
    			} else if (attName.equalsIgnoreCase("corr_type")) {
    				this.currentCorrespondence.corr_type = attValue;
    			} else if (attName.equalsIgnoreCase("user_checked")) {
    				this.currentCorrespondence.nChecked = Integer.parseInt(attValue);
    			}
    		}
    	}
    } else if (qName.equals("object")) {
    	this.currentObject = new String[3];
    	if (attrs!=null) {
    		for (int i=0;i<attrs.getLength();i++) {
    			String attName = attrs.getQName(i);
    			String attValue = attrs.getValue(i).trim();
    			if (attName.equalsIgnoreCase("accession")) {
    				this.currentObject[2] = attValue;
    			} else if (attName.equalsIgnoreCase("objecttype")){
    				this.currentObject[0] = attValue;
    			} else if (attName.equalsIgnoreCase("source_name")){
    				this.currentObject[1] = attValue;
    			} /*
    			else if (attName.equalsIgnoreCase("source")) {
    				String[] tmpSource = attValue.split("@");
    				this.currentObject[1] = tmpSource[1];
    				this.currentObject[0] = tmpSource[0];
    			}*/
    		}
    	}
    	if (stack.peek().equals("domain_objects")) {
    		this.currentCorrespondence.domainObjectType = this.currentObject[0];
    		this.currentCorrespondence.domainSourceName = this.currentObject[1];
    		this.currentCorrespondence.setDomainObjAcc(this.currentObject[2]);
    	} else if (stack.peek().equals("range_objects")) {
    		this.currentCorrespondence.rangeObjectType = this.currentObject[0];
    		this.currentCorrespondence.rangeSourceName = this.currentObject[1];
    		this.currentCorrespondence.setRangeObjAcc(this.currentObject[2]);
    	}
    } else if (qName.equals("params")) {
    	
    } else if (qName.equals("param")||qName.equals("ea_param")) {
    	if (attrs!=null) {
    		for (int i=0;i<attrs.getLength();i++) {
    			String attName = attrs.getQName(i);
    			if (attName.equalsIgnoreCase("name")) {
    				 this.currentAttributeValue = new String[2];
    				 this.currentAttributeValue[0] = attrs.getValue(i).trim();
    			}
    		}
    	}
    } else if (qName.equals("external_access")) {
    	this.mappingToImport.isExternalImport = true;
    	for (int i=0;i<attrs.getLength();i++) {
			String attName = attrs.getQName(i);
			String attValue = attrs.getValue(i).trim();
			if (attName.equalsIgnoreCase("access_class")) {
				this.mappingToImport.accessClass = attValue;
			} else if (attName.equalsIgnoreCase("location")) {
				this.mappingToImport.accessLocation = attValue;
			}
		}
    } else if (qName.equals("corr_att")) {
    	if (attrs!=null) {
    		for (int i=0;i<attrs.getLength();i++) {
    			String attName = attrs.getQName(i);
    			if (attName.equalsIgnoreCase("name")) {
    				if (this.currentAttributeValue==null) 
    					this.currentAttributeValue = new String[3];
    				this.currentAttributeValue[0] = attrs.getValue(i).trim();
    			} else if (attName.equalsIgnoreCase("type")) {
    				if (this.currentAttributeValue==null) 
    					this.currentAttributeValue = new String[3];
    				this.currentAttributeValue[1] = attrs.getValue(i).trim();
    			}
    		}
    	}
    }
    this.stack.push(qName);
  }

  public void endElement(String namespaceURI,String localName,String qName ) throws SAXException {
	  if (qName.equals("correspondence")) {
		this.mappingToImport.addCorrespondence(this.currentCorrespondence);
		this.currentCorrespondence = null;
	  } else if (qName.equals("object")) {
		  this.currentObject = null;
	  }
	  this.stack.pop();
  }

  public void characters( char[] buf, int offset, int len ) throws SAXException {
	  String s = new String( buf, offset, len ).trim();
      if (stack.peek().equals("param")) {
    	  if (this.currentObject!=null&&this.currentAttributeValue!=null) {
   
    	  } else if (this.currentAttributeValue!=null) {
    		  this.currentAttributeValue[1] = s;
    		  this.mappingToImport.addParameter(this.currentAttributeValue[0],this.currentAttributeValue[1]);
    		  this.currentAttributeValue = null;
    	  }
      } else if (stack.peek().equals("ea_param")) {
    	  if (this.currentAttributeValue!=null) {
    		  this.currentAttributeValue[1] = s;
    		  this.mappingToImport.accessProps.put(this.currentAttributeValue[0], this.currentAttributeValue[1]);
    		  this.currentAttributeValue = null;
    	  }
      } else if (stack.peek().equals("corr_att")) {
    	  if (this.currentAttributeValue!=null) {
    		  this.currentAttributeValue[2] = s;
    		  if (this.currentAttributeValue[1].equals("float")) {
    			  float tmpFloat = Float.parseFloat(this.currentAttributeValue[2]);
    			  this.currentCorrespondence.corrAtts.put(this.currentAttributeValue[0], tmpFloat);
    		  } else if (this.currentAttributeValue[1].equals("integer")) {
    			  int tmpInt = Integer.parseInt(this.currentAttributeValue[2]);
    			  this.currentCorrespondence.corrAtts.put(this.currentAttributeValue[0], tmpInt);
    		  } else {
    			  this.currentCorrespondence.corrAtts.put(this.currentAttributeValue[0], this.currentAttributeValue[2]);
    		  }
    		  this.currentAttributeValue = null;
    	  }
      }
  }
}
