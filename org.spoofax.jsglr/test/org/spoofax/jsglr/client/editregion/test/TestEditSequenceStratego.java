package org.spoofax.jsglr.client.editregion.test;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.junit.Test;
import org.spoofax.jsglr.client.InvalidParseTableException;
import org.spoofax.jsglr.client.ParseException;
import org.spoofax.jsglr.client.editregion.detection.EditRegionDetector;
import org.spoofax.jsglr.shared.BadTokenException;
import org.spoofax.jsglr.shared.SGLRException;
import org.spoofax.jsglr.shared.TokenExpectedException;

public class TestEditSequenceStratego extends AbstractTestRegionDetection {

	public TestEditSequenceStratego() throws IOException, InvalidParseTableException, TokenExpectedException, BadTokenException, ParseException, SGLRException{
		pathToJavaTestInputs = "tests-editregions/java/separators";
		setStrategoParser();
	}

	@Test
	public void test() throws FileNotFoundException, IOException {
		lastErr0AST = null;
		String path = "tests-editregions/stratego/edit-sequence";
		for (int i = 0; i < 73; i++) {
			String fname = path + "/edit_"+ i + ".str.scn";
			if(lastErr0AST != null){
				String erroneousInput = loadAsString(fname);
				editRegionRecovery = new EditRegionDetector(lastErr0AST, erroneousInput);
				try {
					parseString(editRegionRecovery.getRecoveredInput());
					System.out.println("recovered: " + fname);
				} 
				catch (Exception e) {
					System.err.println("failed: " + fname);
					System.err.println(editRegionRecovery.getDeletedSubstrings());
					System.err.println(editRegionRecovery.getInsertedSubstrings());
					System.err.println(editRegionRecovery.getEditedRegionsCorrect());
					System.err.println(editRegionRecovery.getEditedRegionsErroneous());
					System.err.println(editRegionRecovery.getEditedTerms());
				} 	
			}
			try {
				lastErr0AST = parseFile(fname);
				System.out.println("err0: " + fname);
			} 
			catch (Exception e) {
				//
			} 	
			
		}
	}

	@Test
	public void test_no_recovery_1() throws FileNotFoundException, IOException, TokenExpectedException, BadTokenException, ParseException, SGLRException {
		String path = "tests-editregions/stratego/edit-sequence";
		String fname_corr = path + "/edit_"+ 26 + ".str.scn";
		String fname_err  = path + "/edit_"+ 27 + ".str.scn";
		lastErr0AST = parseFile(fname_corr);
		String erroneousInput = loadAsString(fname_err);
		editRegionRecovery = new EditRegionDetector(lastErr0AST, erroneousInput);
		try {
			parseString(editRegionRecovery.getRecoveredInput());
			System.out.println("recovered: " + fname_err);
		} 
		catch (Exception e) {
			System.err.println("failed: " + fname_err);
			System.err.println(editRegionRecovery.getDeletedSubstrings());
			System.err.println(editRegionRecovery.getInsertedSubstrings());
			System.err.println(editRegionRecovery.getEditedRegionsCorrect());
			System.err.println(editRegionRecovery.getEditedRegionsErroneous());
			System.err.println(editRegionRecovery.getEditedTerms());
		} 			
	}
}
