package org.spoofax.interpreter.library.jsglr;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.core.InterpreterException;
import org.spoofax.interpreter.core.Tools;
import org.spoofax.interpreter.stratego.Strategy;
import org.spoofax.interpreter.terms.IStrategoString;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.jsglr.InvalidParseTableException;
import org.spoofax.jsglr.ParseTable;
import org.spoofax.jsglr.ParseTableManager;
import org.spoofax.jsglr.SGLR;
import org.spoofax.jsglr.SGLRException;

import aterm.ATerm;
import aterm.ATermFactory;

public class JSGLR_parse_stratego extends JSGLRPrimitive {
	
	private final ATermFactory atermFactory;
	
	private SGLR StrategoSGLR;
	
	JSGLR_parse_stratego(ATermFactory atermFactory) {
		super("JSGLR_parse_stratego", 0, 1);
		this.atermFactory = atermFactory;
	}
	
	@Override
	public boolean call(IContext env, Strategy[] svars,
			IStrategoTerm[] tvars) throws InterpreterException {
		
		if (!Tools.isTermString(tvars[0]))
			return false;
		String path = ((IStrategoString)tvars[0]).stringValue();
		ATerm parsed = null;
		if(StrategoSGLR == null)
			initialize();
		if(StrategoSGLR == null)
			return false;
		try {
			parsed = StrategoSGLR.parse(new BufferedInputStream(new FileInputStream(path)));
		} catch (FileNotFoundException e) {
			System.err.println("File not found.");
			return false;
		} catch (IOException e) {
			System.err.println("IOEXC");
			return false;
		} catch (SGLRException e) {
			System.err.println("SGLR Exc");
			return false;
		}
		if (parsed == null)
			return false;
		env.setCurrent(getATermConverter(env).convert(parsed));
		return true;
	}

	private void initialize() {
		// FIXME this must be cleaned
		ParseTableManager ptm = new ParseTableManager(atermFactory);

		ParseTable pt;
		try {
			pt = ptm.loadFromFile(System.getProperty("share.dir") + "/Stratego.tbl");
			StrategoSGLR = new SGLR(atermFactory, pt);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InvalidParseTableException e) {
			e.printStackTrace();
		}
	}

}
