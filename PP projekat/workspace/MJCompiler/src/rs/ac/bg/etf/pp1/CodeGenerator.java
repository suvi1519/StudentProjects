package rs.ac.bg.etf.pp1;

import rs.ac.bg.etf.pp1.ast.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import rs.ac.bg.etf.pp1.CounterVisitor.*;
import rs.ac.bg.etf.pp1.SemanticPass.ActClass;
import rs.etf.pp1.mj.runtime.Code;
import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Struct;

public class CodeGenerator extends VisitorAdaptor {
	
	//Za slanje
	
	private int varCount;
	private int paramCnt;
	
	private boolean returnFound = false;
	
	private int mainPc;
	
	
	public int getMainPc() {
		return mainPc;
	}
	
	private void lenDef() {
		Obj obj = Tab.find("len");
		obj.setAdr(Code.pc);
		Code.put(Code.enter);
		Code.put(1);
		Code.put(1);
		Code.put(Code.load_n);
		Code.put(Code.arraylength);
		Code.put(Code.exit);
		Code.put(Code.return_);
	}
	
	private void ordDef() {
		Obj obj = Tab.find("ord");
		obj.setAdr(Code.pc);
		Code.put(Code.enter);
		Code.put(1);
		Code.put(1);
		Code.put(Code.load_n);
		Code.put(Code.exit);
		Code.put(Code.return_);
	}
	
	private void chrDef() {
		Obj obj = Tab.find("chr");
		obj.setAdr(Code.pc);
		Code.put(Code.enter);
		Code.put(1);
		Code.put(0);
		Code.put(Code.load_n);
		Code.put(Code.exit);
		Code.put(Code.return_);
	}
	
	
	public void init() {
		lenDef();
		ordDef();
		chrDef();
	}
	
	
	// ======================================================================================================
	// Tipovi i designator 
	
//	public void visit(DesignatorIdent designator) {
//		SyntaxNode parent1 = designator.getParent();
//		SyntaxNode parent2 = parent1.getParent();
////		if (DesignatorOpStmt.class != parent1.getClass() && FactFunc.class != parent1.getClass() || parent2.getClass() == DesignatorArray.class) {
////			Code.load(designator.obj);
////		}
//		if(parent1.getClass() == DesignatorArray.class) {
//			Code.load(designator.obj);
//		}
//	}
////	
//	public void visit(DesignatorArray designator) {
//		Code.load(designator.obj);
//	}
	
	public void visit(DesignatorSimpleIdent designator){
		SyntaxNode parent1 = designator.getParent();
		SyntaxNode parent2 = parent1.getParent();
//		if (DesignatorOpStmt.class != parent2.getClass() && FactFunc.class != parent2.getClass() || designator.obj.getType().getKind() == Struct.Array) {
//			Code.load(designator.obj);
//		}
		if(parent1.getClass() == DesignatorArray.class) {
			Code.load(designator.obj);
		}
//		if(parent2.getClass() == ReadStmt.class) {
//			Code.load(designator.obj);
//		}
	}
	
	public void visit(DesignatorNamespaceIdent designator){
		SyntaxNode parent1 = designator.getParent();
		SyntaxNode parent2 = parent1.getParent();
//		if (DesignatorOpStmt.class != parent2.getClass() && FactFunc.class != parent2.getClass() || designator.obj.getType().getKind() == Struct.Array) {
//			Code.load(designator.obj);
//		}
		if(parent1.getClass() == DesignatorArray.class) {
			Code.load(designator.obj);
		}
	}
	
	public void visit(DesignatorOpStmt desgStmt) {
		Obj des = desgStmt.getDesignator().obj;
		Designator desg = desgStmt.getDesignator();
		DesignatorOp desOp = desgStmt.getDesignatorOp();
		int kind = des.getKind();
		String name = des.getName();
		
		int flagArray = 0;
		
		
		if(desgStmt.getDesignator() instanceof DesignatorArray) {
			flagArray = 1;
		}

		
		
		if(desOp instanceof DesignatorInc || desOp instanceof DesignatorDec) {
			
			if(desOp instanceof DesignatorInc) {
				if(flagArray == 1) {
					Code.put(Code.dup2);
					Code.put(Code.aload);
					Code.loadConst(1);
					Code.put(Code.add);
					Code.put(Code.astore);
				}
				else {
					Code.load(des);
					Code.loadConst(1);
					Code.put(Code.add);
					Code.store(des);
				}
				
			}
			else {
				if(flagArray == 1) {
					Code.put(Code.dup2);
					Code.put(Code.aload);
					Code.loadConst(1);
					Code.put(Code.sub);
					Code.put(Code.astore);
				}
				else {
					Code.load(des);
					Code.loadConst(1);
					Code.put(Code.sub);
					Code.store(des);
				}
				
			}
			
		}
		else if(desOp instanceof DesignatorFunc) {
			int offset = des.getAdr() - Code.pc; 
			Code.put(Code.call);
			Code.put2(offset);
			if(des.getType().getKind() != Struct.None) 
				Code.put(Code.pop);
		}
		else if(desOp instanceof DesignatorAssign) {
			if(flagArray == 1) {
				if(des.getType() == Tab.charType)
					Code.put(Code.bastore);
				else 
					Code.put(Code.astore);
			}
			else {
				Code.store(des);
			}
			
		}
		
	}
	
	class DesigElem{
		Obj obj;
		int type = 0; // 0 - null, 1 - simpleDesig, 2 - arrayDesig
		public DesigElem(Obj o, int t) {
			obj = o;
			type = t;
		}
	}
	
	private ArrayList<DesigElem> desigList = new ArrayList<DesigElem>();
	
	public void visit(Desg desg) {
		if(desg.getDesignator() instanceof DesignatorArray) {
			desigList.add(new DesigElem(desg.getDesignator().obj, 2));
		}
		else {
			desigList.add(new DesigElem(desg.getDesignator().obj, 1));
		}
			
	}
	
	public void visit(NoDesg desg) {
		desigList.add(new DesigElem(null, 0));
	}
	
	public void visit(DesignatorArrayStmt desgStmt) {
		Obj desgRight = desgStmt.getDesignator1().obj;
		Obj desgLeft = desgStmt.getDesignator().obj;
		int charTip = (desgRight.getType().getElemType().getKind() == Struct.Char) ? 1 : 0;
		desigList.remove(desigList.size() - 1);
		int size = desigList.size();
										
		Code.load(desgRight);					//Hvatanje izuzetka
		Code.put(Code.arraylength);
		Code.loadConst(size + 1);
		Code.putFalseJump(Code.ge, 0);
		int adrJmpError = Code.pc - 2;
		
												//Pocetak obrade prvog dela sa nizovima
		Code.load(desgRight);					//Provera duzina nizova i koji se tip bira
		Code.put(Code.arraylength);				//desni >= levog
		Code.load(desgLeft);					//ili
		Code.put(Code.arraylength);				//levi > desnog
		Code.loadConst(size);
		Code.put(Code.add);
		Code.putFalseJump(Code.inverse[Code.lt], 0);
		int adrJmpSizeDetect = Code.pc - 2;
		//==================================
												
		Code.load(desgLeft);					//Ako je desni >= levog
		Code.put(Code.arraylength);				//Levi ide ceo
		
		Code.load(desgLeft);
		Code.put(Code.arraylength);	
		Code.loadConst(size);
		Code.put(Code.add);						//Duzina desnog = size + len(levog)
		Code.putJump(0);
		int adrJmpEnd1 = Code.pc - 2;	
		
		//==================================
		Code.fixup(adrJmpSizeDetect);			//Ako je levi > desnog
		Code.load(desgRight);
		Code.put(Code.arraylength);
		Code.loadConst(size);
		Code.put(Code.sub);						//Duzina levog = len(levog) - size
		
		Code.load(desgRight);					//Desni ide ceo
		Code.put(Code.arraylength);			
		
		Code.fixup(adrJmpEnd1); 				//Podesavanje skoka
		//==================================
		//Prvi deo raspakivanja -> sa nizovima
				
		int adrNizPetlja = Code.pc;				
		Code.loadConst(1);						//Dekrementiranje indeksa oba niza
		Code.put(Code.sub);
		Code.put(Code.dup_x1);
		Code.put(Code.pop);
		Code.loadConst(1);
		Code.put(Code.sub);							
			
		Code.put(Code.dup);						//Provera uslova za kraj - index < 0
		Code.loadConst(0);
		Code.putFalseJump(Code.inverse[Code.lt], 0);
		int adrKrajPrvogDela = Code.pc - 2;
		
		Code.put(Code.dup_x1);					//Nije kraj prvog dela -> unosimo adrese nizova
		Code.put(Code.pop);
		Code.put(Code.dup2);
		Code.load(desgLeft);
		Code.put(Code.dup_x2);
		Code.put(Code.pop);
		Code.load(desgRight);
		Code.put(Code.dup_x1);
		Code.put(Code.pop);
		
		if(charTip == 1) {						//Unos na odgovarajuce mesto u odnosu na tip elemenata
			Code.put(Code.baload);
			Code.put(Code.bastore);
		}
		else {
			Code.put(Code.aload);
			Code.put(Code.astore);
		}
		Code.putJump(adrNizPetlja); 			//Povratak na petlju
		
		Code.fixup(adrKrajPrvogDela); 			//Kraj prvog dela unosa
		Code.put(Code.pop);						//Uklanjanje zaostalog -1 (posle provere uslova)
		
		int index = size - 1;
		DesigElem elem = null;
		while(index >= 0) {
			elem = desigList.remove(index--);	//Nista -> preskace se
			if(elem.type == 0) {
				Code.loadConst(1);
				Code.put(Code.sub);
				continue;
			}
			
			if(elem.type == 1) {				//Simple desig -> nije niz, obicna varijabla je 
				Code.put(Code.dup);
				Code.load(desgRight);
				Code.put(Code.dup_x1);
				Code.put(Code.pop);
				if(charTip == 1) Code.put(Code.baload);
				else Code.put(Code.aload);
				Code.store(elem.obj);
			}
			else {								//Array desig -> niz je
				Code.put(Code.dup_x2);
				Code.load(desgRight);
				Code.put(Code.dup_x1);
				Code.put(Code.pop);
				if(charTip == 1) {
					Code.put(Code.baload);
					Code.put(Code.bastore);
				}
				else {
					Code.put(Code.aload);
					Code.put(Code.astore);
				}
			}
			Code.loadConst(1);
			Code.put(Code.sub);
			
		}
		Code.put(Code.pop);						//Uklanjanje zaostalog -1 (posle zavrsetka obrade)
		
		Code.putJump(0);
		int adrJmpOk = Code.pc-2;
		Code.fixup(adrJmpError);
		Code.put(Code.trap);
		Code.put(1);
		Code.fixup(adrJmpOk);
		
	}
	
	// ======================================================================================================
	// Metode i return
	
	
	public void visit(MethDeclTypeNameVoid MethodTypeName) {
		if ("main".equalsIgnoreCase(MethodTypeName.getMethName())) {
			mainPc = Code.pc;
		}
		MethodTypeName.obj.setAdr(Code.pc);
		
		// Collect arguments and local variables.
		SyntaxNode methodNode = MethodTypeName.getParent();
		VarCounter varCnt = new VarCounter();
		methodNode.traverseTopDown(varCnt);
		FormParamCounter fpCnt = new FormParamCounter();
		methodNode.traverseTopDown(fpCnt);
		
		// Generate the entry.
		Code.put(Code.enter);
		Code.put(fpCnt.getCount());
		Code.put(varCnt.getCount() + fpCnt.getCount());
	}
	
	public void visit(MethDeclTypeNameIdent MethodTypeName) {
		MethodTypeName.obj.setAdr(Code.pc);
		
		// Collect arguments and local variables.
		SyntaxNode methodNode = MethodTypeName.getParent();
		VarCounter varCnt = new VarCounter();
		methodNode.traverseTopDown(varCnt);
		FormParamCounter fpCnt = new FormParamCounter();
		methodNode.traverseTopDown(fpCnt);
		
		// Generate the entry.
		Code.put(Code.enter);
		Code.put(fpCnt.getCount());
		Code.put(varCnt.getCount() + fpCnt.getCount());
	}

	
	public void visit(MethodDeclPart meth) {
		if(returnFound == false) {
			Code.put(Code.exit);
			Code.put(Code.return_);
		}
		returnFound = false;;
	}
	
	public void visit(ReturnValStmt ret) {
		Code.put(Code.exit);
		Code.put(Code.return_);
		returnFound = true;
	}
	

	public void visit(ReturnNoValStmt ret) {
		Code.put(Code.exit);
		Code.put(Code.return_);
		returnFound = true;
	}
	
	// ======================================================================================================
	//Expr, Term, Factor
	
	public void visit(FactNum factNum) {
		Code.load(new Obj(Obj.Con, "$", factNum.struct, factNum.getN1(), 0));
	}
	
	public void visit(FactChar factChar) {
		Code.load(new Obj(Obj.Con, "$", factChar.struct, factChar.getC1(), 0));
	}
	
	public void visit(FactBool factBool) {
		if (factBool.getB1().equals("true")) {
			Code.load(new Obj(Obj.Con, "$", factBool.struct, 1, 0));
		}
		else {
			Code.load(new Obj(Obj.Con, "$", factBool.struct, 0, 0));
		}
		
	}
	
	public void visit(FactDesg factor) {
		if(factor.getDesignator() instanceof DesignatorArray) {
			if(factor.struct.getKind() == Struct.Char)
				Code.put(Code.baload);
			else
				Code.put(Code.aload);
		}
		else Code.load(factor.getDesignator().obj);
	}
	
	
	public void visit(FactorMulti factor) {
		MulOp mulop = factor.getMulOp();
		if(mulop.getClass() == Mul.class) {
			Code.put(Code.mul);
		}
		else if(mulop.getClass() == Div.class) {
			Code.put(Code.div);
		}
		else if(mulop.getClass() == Mod.class) {
			Code.put(Code.rem);
		}
	}
	
	
	public void visit(MinusTerm MinusTerm) {
		Code.put(Code.neg);
	}
	
	public void visit(TermMulti term) {
		AddOp addop = term.getAddOp();
		if(addop.getClass() == Plus.class) {
			Code.put(Code.add);
		}
		else if(addop.getClass() == Minus.class) {
			Code.put(Code.sub);
		}
	}
		
	public void visit(FactFunc funcCall) {
		Obj functionObj = funcCall.getDesignator().obj;
		int offset = functionObj.getAdr() - Code.pc; 
		Code.put(Code.call);
		Code.put2(offset);
	}
	
	
	public void visit(FactNewArray factor) {
		int kind = factor.getType().struct.getKind();
		if(kind == Struct.Char) {
			Code.put(Code.newarray);
			Code.loadConst(0);
		}
		else {
			Code.put(Code.newarray);
			Code.loadConst(1);
		}
		
	}
	
	// ======================================================================================================
	// Conditions
	
	public int getCodeForJmp(RelOp relop) {
		if(relop.getClass() == Equal.class) {
			return Code.eq;
		}
		else if(relop.getClass() == NotEqual.class) {
			return Code.ne;
		}
		else if(relop.getClass() == Greater.class) {
			return Code.gt;
		}
		else if(relop.getClass() == GreaterEq.class) {
			return Code.ge;
		}
		else if(relop.getClass() == Less.class) {
			return Code.lt;
		}
		else if(relop.getClass() == LessEq.class) {
			return Code.le;
		}
		return -1;
	}
	
	class IfStmtClass{
		ArrayList<Integer> izaThen = new ArrayList<>();
		ArrayList<Integer> naThen = new ArrayList<>();
		ArrayList<Integer> orCond = new ArrayList<>();
		ArrayList<Integer> izaElse = new ArrayList<>();
	}
	
	ArrayList<IfStmtClass> ifStmtList = new ArrayList<>();
	
	public void visit(IfBegin ifb) {
		ifStmtList.add(new IfStmtClass());
	}
	
	public void visit(IfStmt ifs) {
		ifStmtList.remove(ifStmtList.size() - 1);
	}
	
	public void visit(ExprRelopMulti exprRelopMulti) {
		IfStmtClass ifs = ifStmtList.get(ifStmtList.size() - 1);
		RelOp relop = exprRelopMulti.getRelOp();
		int relCode = getCodeForJmp(relop);
		if(exprRelopMulti.getParent().getParent() instanceof CondFactSingle) {
			Code.putFalseJump(Code.inverse[relCode], 0);
			int adr = Code.pc - 2;
			ifs.naThen.add(adr);
		}
		else {
			Code.putFalseJump(relCode, 0);
			int adr = Code.pc - 2;
			ifs.orCond.add(adr);
		}
	}
		
	public void visit(ExprRelopSingle exprRelopSingle) {
		IfStmtClass ifs = ifStmtList.get(ifStmtList.size() - 1);
		Code.loadConst(0);
		if(exprRelopSingle.getParent().getParent() instanceof CondFactSingle) {
			Code.putFalseJump(Code.eq, 0);
			int adr = Code.pc - 2;
			ifs.naThen.add(adr);
		}
		else {
			Code.putFalseJump(Code.ne, 0);
			int adr = Code.pc - 2;
			ifs.orCond.add(adr);
		}
	}
	
	
	public void visit(CondTerm condTerm) {
		IfStmtClass ifs = ifStmtList.get(ifStmtList.size() - 1);
		for(int i = ifs.orCond.size() - 1; i >= 0; i--) {
			int rep = ifs.orCond.remove(i);
			Code.fixup(rep);
		}
	}
	
	public void visit(Conditions condition) {
		IfStmtClass ifs = ifStmtList.get(ifStmtList.size() - 1);
		Code.putJump(0);
		int adr = Code.pc - 2;
		ifs.izaThen.add(adr);
		for(int i = ifs.naThen.size() - 1; i >= 0; i--) {
			int rep = ifs.naThen.remove(i);
			Code.fixup(rep);
		}
	}
	
	
	// ======================================================================================================
	// Statementi -> print, read
	
	public void visit(PrintNoSizeStmt print) {
		if(print.getExpr().struct == Tab.charType || print.getExpr().struct.getElemType() == Tab.charType) {
			Code.loadConst(1);
			Code.put(Code.bprint);
		}
		else {
			Code.loadConst(5);
			Code.put(Code.print);
		}
	}
	
	public void visit(PrintSizeStmt print) {
		Code.loadConst(print.getN2());
		if(print.getExpr().struct == Tab.charType || print.getExpr().struct.getElemType() == Tab.charType) {
			Code.put(Code.bprint);
		}
		else {
			Code.put(Code.print);
		}
	}
	
	public void visit(ReadStmt readStmt) {
		Obj des = readStmt.getDesignator().obj;
		if(des.getType().getKind() == Struct.Char) {
			Code.put(Code.bread);
		}
		else {
			Code.put(Code.read);
		}
		
		if(readStmt.getDesignator().getClass() == DesignatorArray.class) {
			if(des.getType().getKind() == Struct.Char) {
				Code.put(Code.bastore);
			}
			else {
				Code.put(Code.astore);
			}
		}
		else
			Code.store(des);
		
	}
	
	// ======================================================================================================
	// Statementi -> if, else	
	
	public void visit(ElseBegin e) {
		IfStmtClass ifs = ifStmtList.get(ifStmtList.size() - 1);
		Code.putJump(0);
		int adr = Code.pc - 2;
		ifs.izaElse.add(adr);
		for(int i = ifs.izaThen.size() - 1; i >= 0; i--) {
			int rep = ifs.izaThen.remove(i);
			Code.fixup(rep);
		}
	}
	
	public void visit(ElseStmt e) {
		IfStmtClass ifs = ifStmtList.get(ifStmtList.size() - 1);
		for(int i = ifs.izaElse.size() - 1; i >= 0; i--) {
			int rep = ifs.izaElse.remove(i);
			Code.fixup(rep);
		}
	}
	
	public void visit(NoElseStmt e) {
		IfStmtClass ifs = ifStmtList.get(ifStmtList.size() - 1);
		for(int i = ifs.izaThen.size() - 1; i >= 0; i--) {
			int rep = ifs.izaThen.remove(i);
			Code.fixup(rep);
		}
	}
	
	// ======================================================================================================
	// Statementi -> for, break, continue...
	
	class ForStmtClass{
		int forCond;
		int naPost;
		int naTelo;
		ArrayList<Integer> breakList = new ArrayList<>();
	}
	
	ArrayList<ForStmtClass> forStmtList = new ArrayList<>();
	
	public void visit(ForBegin fb) {
		ifStmtList.add(new IfStmtClass());
		forStmtList.add(new ForStmtClass());
	}
	
	
	public void visit(DesignatorStatementListPre dslp) {
		ForStmtClass fsc = forStmtList.get(forStmtList.size() - 1);
		int adr = Code.pc;
		fsc.forCond = adr;
	}
	
	public void visit(ConditionFactFor cff) {
		ForStmtClass fsc = forStmtList.get(forStmtList.size() - 1);
		Code.putJump(0);
		int adr1 = Code.pc - 2;
		fsc.naTelo = adr1;
		int adr2 = Code.pc;
		fsc.naPost = adr2;
	}
	
	public void visit(NoConditionFactFor ncff) {
		ForStmtClass fsc = forStmtList.get(forStmtList.size() - 1);
		Code.putJump(0);
		int adr1 = Code.pc - 2;
		fsc.naTelo = adr1;
		int adr2 = Code.pc;
		fsc.naPost = adr2;
	}
	
	public void visit(DesignatorStatementListPost dslp) {
		ForStmtClass fsc = forStmtList.get(forStmtList.size() - 1);
		int adr = fsc.forCond;
		Code.putJump(adr);
		
		int adr2 = fsc.naTelo;
		Code.fixup(adr2);
	}
	
	public void visit(ForStmt fs) {
		ForStmtClass fsc = forStmtList.get(forStmtList.size() - 1);
		IfStmtClass ifs = ifStmtList.get(ifStmtList.size() - 1);
		int adr = fsc.naPost;
		Code.putJump(adr);
		
		for(int i = ifs.izaThen.size() - 1; i >= 0; i--) {
			int rep = ifs.izaThen.get(i);
			Code.fixup(rep);
		}
		
		for(int i = 0; i < fsc.breakList.size(); i++) {
			Code.fixup(fsc.breakList.get(i));
		}
		
		ifStmtList.remove(ifStmtList.size() - 1);
		forStmtList.remove(forStmtList.size() - 1);
	}
	
	
	public void visit(ContinueStmt cs) {
		ForStmtClass fsc = forStmtList.get(forStmtList.size() - 1);
		int adr = fsc.naPost;
		Code.putJump(adr);
	}
	
	public void visit(BreakStmt bs) {
		ForStmtClass fsc = forStmtList.get(forStmtList.size() - 1);
		Code.putJump(0);
		int adr = Code.pc - 2;
		fsc.breakList.add(adr);
	}
	
	
	
}
