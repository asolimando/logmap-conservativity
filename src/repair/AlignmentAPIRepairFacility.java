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
package repair;

import java.util.Properties;
import java.util.Set;

import logmap.LogMapWrapper;

import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.AlignmentRepairer;
import org.semanticweb.owl.align.Cell;
import org.semanticweb.owl.align.OntologyNetwork;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import enumerations.REASONER_KIND;
import enumerations.REPAIR_METHOD;
import fr.inrialpes.exmo.align.impl.ObjectAlignment;
import uk.ac.ox.krr.logmap2.mappings.objects.MappingObjectStr;
import util.OntoUtil;
import util.Params;

public class AlignmentAPIRepairFacility implements AlignmentRepairer { 
	
		public static final String reasonerParamKey = "reasoner"; 
		public static final String reasonerParamValueHermit = "hermit";
		public static final String reasonerParamValuePellet = "pellet";
		public static final String reasonerParamValueELK = "elk";
		
		public static final String conservRepairKindParamKey = "conservRepairKind";
		public static final String conservRepairKindParamValueEQ = "eq";
		public static final String conservRepairKindParamValueSUB = "sub";
		public static final String conservRepairKindParamValueSUBEQ = "sub_eq";
		public static final String conservRepairKindParamValueEQSUB = "eq_sub";
		
		public static final String alignOntoClassificationTimeoutSecsKey = "alignOntoClassificationTimeout";
		public static final String inputOntoClassificationTimeoutSecsKey = "inputOntoClassificationTimeout";
		
		public void init(Properties param) throws AlignmentException {
			
			if(param == null || param.isEmpty())
				return;
			
			if(param.containsKey(reasonerParamKey)){
				String reasoner = param.getProperty(reasonerParamKey);
				switch (reasoner) {
				case reasonerParamValueHermit:
					Params.reasonerKind = REASONER_KIND.HERMIT;
					break;
				case reasonerParamValuePellet:
					Params.reasonerKind = REASONER_KIND.PELLET;
					break;
				case reasonerParamValueELK:
					Params.reasonerKind = REASONER_KIND.ELK;
					break;
				default:
					throw new AlignmentException("Unsupported reasoner: " + reasoner);
				}
			}
			
			if(param.containsKey(conservRepairKindParamKey)){
				String kind = param.getProperty(conservRepairKindParamKey);
				switch(kind){
				case conservRepairKindParamValueEQ:
					Params.defaultRepairMethod = REPAIR_METHOD.EQ;
					break;
				case conservRepairKindParamValueSUB:
					Params.defaultRepairMethod = REPAIR_METHOD.SUB;
					break;
				case conservRepairKindParamValueEQSUB:
					Params.defaultRepairMethod = REPAIR_METHOD.EQSUB;
					break;
				case conservRepairKindParamValueSUBEQ:
					Params.defaultRepairMethod = REPAIR_METHOD.SUBEQ;
					break;
				default:
					throw new AlignmentException(
							"Unsupported conservativity repair kind: " + kind);
				}
			}

			if(param.containsKey(alignOntoClassificationTimeoutSecsKey)){
				String timeout = param.getProperty(alignOntoClassificationTimeoutSecsKey); 
				try {
					Params.alignOntoClassificationTimeout = Integer.parseInt(timeout);
				}
				catch(NumberFormatException e){
					System.err.println("Ignoring invalid timeout value: " + timeout);
				}
			}
			
			if(param.containsKey(inputOntoClassificationTimeoutSecsKey)){
				String timeout = param.getProperty(inputOntoClassificationTimeoutSecsKey); 
				try {
					Params.inputOntoClassificationTimeout = Integer.parseInt(timeout);
				}
				catch(NumberFormatException e){
					System.err.println("Ignoring invalid timeout value: " + timeout);
				}
			}
		};
		
		public Alignment repair( Alignment alinit, Properties param ) throws AlignmentException {

			init(param);
			
//			if ( !(alinit.getOntology1() instanceof OWLOntology) 
//					|| !(alinit.getOntology2() instanceof OWLOntology) )
//				throw new AlignmentException( "Can only repair OWLOntology" );
//	
//			OWLOntology onto1 = (OWLOntology)alinit.getOntology1();
//			OWLOntology onto2 = (OWLOntology)alinit.getOntology2();

			OWLOntologyManager manager = OntoUtil.getManager(true);
			OWLOntology onto1 = null, onto2 = null;
			try {
				onto1 = OntoUtil.load(alinit.getOntology1().toString(), 
						true, manager);
				onto2 = OntoUtil.load(alinit.getOntology2().toString(), 
						true, manager);

			} catch (OWLOntologyCreationException e) {
				e.printStackTrace();
				return null;
			}

			Set<MappingObjectStr> input_mappings = 
					LogMapWrapper.convertToLogMapAlignment(onto1, onto2, alinit);
	
			// ** Here taking the arguments from the parameters if they exists would be best
	
	
			//Set of mappings repaired by LogMap
			ConservativityRepairFacility repairFac = 
					new ConservativityRepairFacility(onto1, onto2, 
							OntoUtil.getManager(false), input_mappings);
	
			repairFac.repair(false);
			Set<MappingObjectStr> repaired_mappings = repairFac.getRepairedMappings();
			
			ObjectAlignment al = new ObjectAlignment();
			al.init(onto1, onto2);

			for (MappingObjectStr m : repaired_mappings) {
				Cell c = LogMapWrapper.convertToCell(m);
				al.addAlignCell(
						c.getObject1(),
						c.getObject2(),
						c.getRelation().toString(),
						m.getConfidence()
				);
			}
			
			repairFac.dispose();
			
			return al;
		}
		
		public OntologyNetwork repair(OntologyNetwork network, Properties param) 
				throws AlignmentException {
			throw new AlignmentException( "Not implemented yet" );
		}
}
