package org.spoofax.jsglr2.parsetable;

import static org.spoofax.terms.StrategoListIterator.iterable;
import static org.spoofax.terms.Term.applAt;
import static org.spoofax.terms.Term.intAt;
import static org.spoofax.terms.Term.isTermAppl;
import static org.spoofax.terms.Term.javaString;
import static org.spoofax.terms.Term.termAt;

import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoConstructor;
import org.spoofax.interpreter.terms.IStrategoList;
import org.spoofax.interpreter.terms.IStrategoNamed;
import org.spoofax.interpreter.terms.IStrategoString;
import org.spoofax.interpreter.terms.IStrategoTerm;

public class ProductionReader {
	
	public static Production read(IStrategoTerm numberedProductionTerm) throws ParseTableReadException{
		IStrategoAppl productionTerm = termAt(numberedProductionTerm, 0); // A tuple of the production right hand side, left hand side and attributes
		int productionNumber = intAt(numberedProductionTerm, 1);
		
		IStrategoAppl lhs = termAt(productionTerm, 1);
		IStrategoList rhs = termAt(productionTerm, 0);
		IStrategoAppl attributesTerm = termAt(productionTerm, 2);
		
		ProductionAttributes attributes = readProductionAttributes(attributesTerm); // Attributes stored in a separate term
		
		String sort = getSort(lhs);
		boolean isLayout = getIsLayout(lhs);
		boolean isLiteral = getIsLiteral(lhs);
		boolean isLexical = getIsLexical(lhs, rhs);
		boolean isList = getIsList(lhs, attributes.isFlatten);
		boolean isOptional = getIsOptional(lhs);
		boolean isOperator = getIsOperator(lhs, isLiteral);
		
		boolean isContextFree = !(isLayout || isLiteral || isLexical);
		
		return new Production(productionNumber, sort, isContextFree, isLayout, isLiteral, isLexical, isList, isOptional, isOperator, attributes);
	}
	
	private static String getSort(IStrategoAppl lhs) {
		for (IStrategoTerm current = lhs; current.getSubtermCount() > 0 && isTermAppl(current); current = termAt(current, 0)) {
    			IStrategoAppl currentAppl = (IStrategoAppl) current;
			String sort = tryGetSort(currentAppl);
			
			if (sort != null)
				return sort;
	    	}
	    	
	    	return null;
	}

	private static String tryGetSort(IStrategoAppl appl) {
		IStrategoConstructor cons = appl.getConstructor();
		
		if ("sort".equals(cons.getName()))
			return javaString(termAt(appl, 0));
		else if ("cf".equals(cons.getName()) || "lex".equals(cons.getName()))
			return tryGetSort(applAt(appl, 0));
		else if ("parameterized-sort".equals(cons.getName()))
			return getParameterizedSortName(appl);
		else if ("char-class".equals(cons.getName()))
			return null;
		else if ("alt".equals(cons.getName()))
			return getAltSortName(appl);
		else
			return null;
	}
	
	private static final int PARAMETRIZED_SORT_NAME = 0;
	private static final int PARAMETRIZED_SORT_ARGS = 1;
    
    private static String getParameterizedSortName(IStrategoAppl parameterizedSort) {
	    	StringBuilder result = new StringBuilder();
	    	
	    	result.append(((IStrategoNamed) termAt(parameterizedSort, PARAMETRIZED_SORT_NAME)).getName());
	    	result.append('_');
    	
		IStrategoList args = termAt(parameterizedSort, PARAMETRIZED_SORT_ARGS);
		
        for (IStrategoTerm arg : iterable(args)) {
			result.append(((IStrategoNamed) arg).getName());
		}
		
		return result.toString();
    }

	private static final int ALT_SORT_LEFT = 0;
	private static final int ALT_SORT_RIGHT = 1;
	
    private static String getAltSortName(IStrategoAppl node) {
		String left = getSort(applAt(node, ALT_SORT_LEFT));
		String right = getSort(applAt(node, ALT_SORT_RIGHT));
		
		return left + "_" + right + "0";
    }

	public static boolean getIsLayout(IStrategoTerm lhs) {
		IStrategoTerm details = termAt(lhs, 0);
		
		if (!isTermAppl(details))
			return false;
		
		if ("opt".equals(((IStrategoAppl) details).getConstructor().getName()))
			details = termAt(details, 0);
		
		return "layout".equals(((IStrategoAppl) details).getConstructor().getName());
	}

	public static boolean getIsLiteral(IStrategoAppl lhs) {
		String constructorName = lhs.getConstructor().getName();
		
		return "lit".equals(constructorName) || "cilit".equals(constructorName);
	}

	public static boolean getIsLexical(IStrategoAppl lhs, IStrategoList rhs) {
		String constructorName = lhs.getConstructor().getName();
	    
		return "lex".equals(constructorName) || getIsLexicalRhs(rhs);
	}
	
	public static boolean getIsLexicalRhs(IStrategoList rhs) {
		if (rhs.getSubtermCount() > 0) {
	        boolean lexRhs = true;
	        
	        for (IStrategoTerm rhsPart : rhs.getAllSubterms()) {
	            String rhsPartConstructor = ((IStrategoAppl) rhsPart).getConstructor().getName();
	            
	            lexRhs &= "char-class".equals(rhsPartConstructor);
	        }
	        
	        return lexRhs;
	    } else
	        return false;
	}
	
	public static boolean getIsList(IStrategoAppl lhs, boolean isFlatten) {
		IStrategoConstructor constructor = getIterConstructor(lhs);
		
		return getIsIterFun(constructor) || "seq".equals(constructor.getName()) || isFlatten;
	}
	
	private static IStrategoConstructor getIterConstructor(IStrategoAppl lhs) {
	    IStrategoAppl details = lhs;
        
        if ("varsym".equals(details.getConstructor().getName()))
            details = termAt(details, 0);
        
        if ("cf".equals(details.getConstructor().getName()))
            details = termAt(details, 0);
        
        if ("opt".equals(details.getConstructor().getName()))
            details = termAt(details, 0);
        
        return details.getConstructor();
	}

	public static boolean getIsIterFun(IStrategoConstructor constructor) {
		String constructorName = constructor.getName();
		
		return "iter".equals(constructorName) || "iter-star".equals(constructorName) || "iter-plus".equals(constructorName) || "iter-sep".equals(constructorName) || "iter-star-sep".equals(constructorName) || "iter-plus-sep".equals(constructorName);
	}

	public static boolean getIsOptional(IStrategoAppl lhs) {
		if ("opt".equals(lhs.getConstructor().getName()))
			return true;
		
		IStrategoTerm contents = termAt(lhs, 0);
		
		return contents.getSubtermCount() == 1 && isTermAppl(contents) && "opt".equals(((IStrategoAppl) contents).getConstructor().getName());
	}
    
    public static boolean getIsOperator(IStrategoAppl lhs, boolean isLiteral) {
    		// An operator literal is always a literal
        if (!isLiteral)
        		return false;
        
        IStrategoString lit = termAt(lhs, 0);
        String contents = lit.stringValue();
        
        // Operators are literals with all characters not being letters
        for (int i = 0; i < contents.length(); i++) {
            char c = contents.charAt(i);
            
            if (Character.isLetter(c))
            		return false;
        }
        
        return true;
    }
	
	private static ProductionAttributes readProductionAttributes(IStrategoAppl attributesTerm) throws ParseTableReadException {
		if (attributesTerm.getName().equals("attrs")) {
			ProductionType type = ProductionType.NO_TYPE;

            IStrategoTerm constructor = null;
            
			boolean isRecover = false;
			boolean isBracket = false;
			boolean isCompletion = false;
			boolean isPlaceholderInsertion = false;
			boolean isLiteralCompletion = false;
			boolean isIgnoreLayout = false;
			boolean isNewlineEnforced = false;
			boolean isLongestMatch = false;
			boolean isCaseInsensitive = false;
			boolean isIndentPaddingLexical = false;
			boolean isFlatten = false;
            
            IStrategoList attributesTermsList = (IStrategoList) attributesTerm.getSubterm(0);
            
            for (IStrategoTerm attributeTerm : attributesTermsList) {
	            	IStrategoNamed attributeTermNamed = (IStrategoNamed) attributeTerm;
	            	
	            	String attributeName = attributeTermNamed.getName();
	            	
	            	switch (attributeName) {
	            		case "reject":
	            			type = ProductionType.REJECT;
	            			break;
	            		case "prefer":
	            			type = ProductionType.PREFER;
	            			break;
	            		case "avoid":
	            			type = ProductionType.AVOID;
	            			break;
	            		case "bracket":
	            			type = ProductionType.BRACKET;
		            		isBracket = true;
		            		break;
	            		case "assoc":
	            			IStrategoNamed associativityAtttributeTermNamed = (IStrategoNamed) attributeTermNamed.getSubterm(0);
		        			String associativityName = associativityAtttributeTermNamed.getName();
	            			
		        			if (associativityName.equals("left") || associativityName.equals("assoc")) {
	                        type = ProductionType.LEFT_ASSOCIATIVE;
	                    } else if (associativityName.equals("right")) {
	                        type = ProductionType.RIGHT_ASSOCIATIVE;
	                    }
	            			break;
	            		case "term":
	            			if (attributeTermNamed.getSubterm(0) instanceof IStrategoNamed) {
		        				IStrategoNamed attributeValueTermNamed = (IStrategoNamed) attributeTermNamed.getSubterm(0);
		        				
		        				int subtermCount = attributeValueTermNamed.getSubtermCount();
		        				String name = attributeValueTermNamed.getName();
		        				
		        				if (subtermCount == 0) {
		        					switch (name) {
		        						case "recover":
			        						isRecover = true;
			        						break;
		        						case "completion":
		        							isCompletion = true;
			        						break;
		        						case "placeholder-insertion":
		        							isPlaceholderInsertion = true;
			        						break;
		        						case "literal-completion":
		        							isLiteralCompletion = true;
			        						break;
		        						case "ignore-layout":
		        						case "ignore-indent":
		        							isIgnoreLayout = true;
			        						break;
		        						case "enforce-newline":
		        							isNewlineEnforced = true;
			        						break;
		        						case "longest-match":
		        							isLongestMatch = true;
			        						break;
		        						case "case-insensitive":
		        							isCaseInsensitive = true;
			        						break;
		        						case "indentpadding":
		        							isIndentPaddingLexical = true;
			        						break;
		        						case "flatten":
		        							isFlatten = true;
			        						break;
		        						default:
		        							break;
		        					}
		        				} else if (subtermCount == 1 && name.equals("cons") && constructor == null) {
		        					constructor = attributeValueTermNamed.getSubterm(0);
		        				}
		        			}
	            			break;
	            		case "id":
	            			constructor = attributeTermNamed.getSubterm(0);
	            			break;
	            		default:
	            			throw new ParseTableReadException("Unknown production attribute: " + attributeName);
	            	}
            }
            
            return new ProductionAttributes(type, constructor, isRecover, isBracket, isCompletion, isPlaceholderInsertion, isLiteralCompletion, isIgnoreLayout, isNewlineEnforced, isLongestMatch, isCaseInsensitive, isIndentPaddingLexical, isFlatten);
		} else if (attributesTerm.getName().equals("no-attrs")) {
            return new ProductionAttributes(ProductionType.NO_TYPE, null, false, false, false, false, false, false, false, false, false, false, false);
        }
		
		throw new ParseTableReadException("Unknown production attribute type: " + attributesTerm);
	}
	
	
}