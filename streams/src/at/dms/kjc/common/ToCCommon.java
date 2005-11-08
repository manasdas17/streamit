package at.dms.kjc.common;

import java.io.StringWriter;
import java.util.Vector;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.LoweringConstants;
import at.dms.compiler.TabbedPrintWriter;
import at.dms.compiler.JavaStyleComment;
/**
 * Somewhat artificial class to provide common code for 
 * at.dms.kjc.common.ToC and at.dms.kjc.lir.LIRToC
 *
 * Hopefully keep from having to fix some bugs twice.
 *
 * @author Allyn Dimock
 */

public abstract class ToCCommon extends SLIREmptyVisitor {
	
    /**
     *  Controls visitPrintStatement. Can override for a backend by writing
     *  to this variable. initialized by a static block.  Can be
     *  overridden in a static block in a subclass by 
     *  printPrefixMap.clear();
     *  printPrefixMap.put...
     *  
     *  If there is more than one printer inheriting from ToCCommon and 
     *  your inheritance structure is not linear then you may have
     *  to write to printPrefixMap from a constructor rather than from
     *  a static block. 
     *
     *  If your backend does more on prints than the common
     *  backend, you will probably want to override printExp.
     */

    static protected Map/*<String><String>*/ printPrefixMap;
    
    /**
     * Print postfixes: defaults to ");" useful for printing boolean.
     */
    static protected Map/*<String><String>*/ printPostfixMap;

    static {
    	printPrefixMap = new java.util.HashMap();
    	printPostfixMap = new java.util.HashMap();
    	// Set up standard prefixes for visitPrintStatement.
	// a subclass may override by:
	// printPrefixMap.clear(); printPrefixMap.put...
	printPrefixMap.put("boolean", "printf( \"%s\", ");
	printPostfixMap.put("boolean", " ? \"true\" : \"false\");");
	printPrefixMap.put("byte", "printf( \"%d\", ");
    	printPrefixMap.put("char", "printf( \"%c\", ");
    	printPrefixMap.put("double", "printf( \"%f\", ");
    	printPrefixMap.put("float", "printf( \"%f\", ");
    	printPrefixMap.put("int", "printf( \"%d\", ");
    	printPrefixMap.put("long", "printf( \"%d\", ");
    	printPrefixMap.put("short", "printf( \"%d\", ");
    	printPrefixMap.put("java.lang.String", "printf( \"%s\", ");
    	// we don't currently print:  bit, or any CCLassType other
    	// than String

    }
    
    /** Needed to pass info from assignment to visitNewArray **/
    protected JExpression lastLeft;  // LITtoC gave package visibility
    /** tabbing / spacing variables **/
    protected int TAB_SIZE = 2;
    /** tabbing / spacing variables **/
    protected int WIDTH = 80;
    /** tabbing / spacing variables **/
    protected int pos;
    /** Some output classes **/
    protected TabbedPrintWriter p;
    protected StringWriter str;

    /**
     * set statmentContext to true in statments, false in expressions
     * Used to control parentheses:  Specifically, an assignment
     * expression used in a statment context should not be wrapped
     * with parentheses since some assignments in staement context
     * are rewritten to multple statements.
     */
    protected boolean statementContext = true;

    /**
     * With no parameters: create a new TabbedPrintWriter for output
     */
    protected ToCCommon() {
	this.str = new StringWriter();
        this.p = new TabbedPrintWriter(str);
    }

    /**
     * With a TabbedPrintWriter: use the given TabbedPrintWriter for output
     * and start off with no indentation yet.
     */
    protected ToCCommon(TabbedPrintWriter p) {
	this.str = null;
        this.p = p;
	this.pos = 0;
    }



    // ----------------------------------------------------------------------
    // METHODS for TabbedPrintWriter p
    // ----------------------------------------------------------------------

    protected void newLine() {
        p.println();
    }

    public void print(String s) {
        p.setPos(pos);
        p.print(s);
    }

    protected void print(Object s) {
        print(s.toString());
    }

    protected void print(boolean s) {
        print("" + s);
    }

    protected void print(int s) {
        print("" + s);
    }

    protected void print(char s) {
        print("" + s);
    }

    protected void print(double s) {
        print("" + s);
    }

    /**
     * Print a left parenthesis if not in statement context.
     */
    protected void printLParen() {
	if (! statementContext) {
	    print("(");
	}
    }

    /**
     * Print a right parenthesis if not in statement context.
     */
    protected void printRParen() {
	if (! statementContext) {
	    print(")");
	}
    }


    // ------------------------------------------------------------------------
    // METHODS for StringWriter str
    // ------------------------------------------------------------------------

    public String getString() {
        if (str != null)
            return str.toString();
        else
            return null;
    }
    

    public void setPos(int pos) {
        this.pos = pos;
    }



    // ------------------------------------------------------------------------
    // More substantial common methods.
    // ------------------------------------------------------------------------

    /**
     * prints an array allocator expression
     *
     * Uses malloc or calloc based on setting of KjcOptions.malloczeros
     * We seem to always have KjcOptions.malloczeros == true
     * The RAW backend has problems with non-zeroed arrays so don't
     *  set KjcOptions.malloczeros to false!
     */
    public void visitNewArrayExpression(JNewArrayExpression self,
                                        CType type,
                                        JExpression[] dims,
                                        JArrayInitializer init
					)
    {
	print(" /* ToCCommon visitNewArrayExpression "
      	      + this.getClass().getName() + "*/ ");
	//the memory allocator to use: 
	String memory_alloc = KjcOptions.malloczeros ? 
	    "malloc" : "calloc";
	//malloc takes one arg, calloc two, so use a different sep between
	//size and elements
	String mem_alloc_sep = KjcOptions.malloczeros ? 
	    " * " : ", ";


	int derefs = dims.length; // number of *'s after element type
	Vector suffixes = new Vector();
	for (int dim = 0; dim < dims.length; dim++) {
	    print("(");		// cast return type of allocator
	    print(type);
	    for (int i = 0; i < derefs; i++) { print("*"); }
	    print(")");
	    print(memory_alloc + "("); // allocation
	    dims[dim].accept(this);
	    print(mem_alloc_sep + "sizeof(");
	    print(type);
	    for (int i = 0; i < derefs - 1; i++) { print("*"); }
	    print("));");
	    newLine();
	    // now either allocate subarrays if not last dimension
	    // or optionally initialize if last dimension;
	    if (dim == dims.length - 1) {
		// gotten to data in array: 
		// code creating initialization code not yet written
		if (init != null) {
		    print("/* initialize with "); 
		    init.accept(this);
		    print("*/");
		    print(" no initialization code! ");
		    newLine();
		}
		break;
            }
	    // initialize sub-arrays in for loop so that large arrays
	    // don't result in large amounts of C code
	    String indx = LoweringConstants.getUniqueVarName();
	    print("{");
	    newLine();
	    pos += TAB_SIZE;
	    print("int " + indx + ";");
	    newLine();
	    print("for (" + indx + "= 0; " 
		  + indx + " < ");
	    derefs--;		// drop a level of indirection each time
            dims[dim].accept(this);
            print("; " 
		  + indx + "++) {");
	    newLine();
	    pos += TAB_SIZE;
	    suffixes.add("[" + indx + "]");
	    lastLeft.accept(this);
	    for (int i = 0; i < suffixes.size(); i++) {
		print((String)(suffixes.get(i)));
	    }
            print(" = ");
	}
	    
	// close off any for loops created.
	for (int dim = 0; dim < dims.length-1; dim++) {
	    pos -= TAB_SIZE;	// end of emitted for loop
	    print("}");
	    newLine();
	    pos -= TAB_SIZE;	// end of emitted block declaring loop variable
	    print("}");
	    newLine();
	}
    }

// ----------------------------------------------------------------------------
// Statements common to ToC, LIRToC up to white space
// ----------------------------------------------------------------------------

    /**
     * prints a while statement
     */
    public void visitWhileStatement(JWhileStatement self,
                                    JExpression cond,
                                    JStatement body) {
	boolean oldStatementContext = statementContext;
	newLine();
        print("while (");
	statementContext = false;
        cond.accept(this);
        print(") ");
	statementContext = true;
        body.accept(this);
	statementContext = oldStatementContext;
    }

    /**
     * prints a variable declaration statement
     */
    public void visitVariableDeclarationStatement(JVariableDeclarationStatement self,
                                                  JVariableDefinition[] vars) {
	boolean oldStatementContext = statementContext;
	statementContext = false;
        for (int i = 0; i < vars.length; i++) {
            vars[i].accept(this);
        }
	statementContext = oldStatementContext;
    }

    /**
     * prints a switch statement
     */
    public void visitSwitchStatement(JSwitchStatement self,
                                     JExpression expr,
                                     JSwitchGroup[] body) {
	boolean oldStatementContext = statementContext;
        print("switch (");
	statementContext = false;
        expr.accept(this);
        print(") {");
	statementContext = true;
        for (int i = 0; i < body.length; i++) {
            body[i].accept(this);
        }
        newLine();
        print("}");
	statementContext = oldStatementContext;
    }

    /**
     * prints a return statement
     */
    public void visitReturnStatement(JReturnStatement self,
                                     JExpression expr) {
	boolean oldStatementContext = statementContext;
        print("return");
        if (expr != null) {
            print(" ");
	    statementContext = false;
            expr.accept(this);
        }
        print(";");
	statementContext = oldStatementContext;
    }

    /**
     * prints a labeled statement
     */
    public void visitLabeledStatement(JLabeledStatement self,
                                      String label,
                                      JStatement stmt) {
        print(label + ":");
        stmt.accept(this);
    }

    /**
     * prints a compound statement: 2-argument form
     */
    public void visitCompoundStatement(JCompoundStatement self,
                                       JStatement[] body) {
	boolean oldStatementContext = statementContext;
	statementContext = true;
        visitCompoundStatement(body);
	statementContext = oldStatementContext;
    }

    /**
     * prints an expression statement
     */
    public void visitExpressionStatement(JExpressionStatement self,
                                         JExpression expr) {
	boolean oldStatementContext = statementContext;
	statementContext = true;
        expr.accept(this);
	print(";");
	statementContext = oldStatementContext;
    }

    /**
     * prints an expression list statement
     */
    public void visitExpressionListStatement(JExpressionListStatement self,
                                             JExpression[] expr) {
	boolean oldStatementContext = statementContext;
	// Want expressions parenthesized here to not have problems with
	// relative precedence with ","
	statementContext = false;
        for (int i = 0; i < expr.length; i++) {
            if (i != 0) {
                print(", ");
            }
            expr[i].accept(this);
        }
	statementContext = oldStatementContext;
	print(";");
    }

    /**
     * prints a do statement
     */
    public void visitDoStatement(JDoStatement self,
                                 JExpression cond,
                                 JStatement body) {
	boolean oldStatementContext = statementContext;
        newLine();
        print("do ");
	statementContext = true;
        body.accept(this);
        print("");
        print("while (");
	statementContext = false;
        cond.accept(this);
        print(");");
	statementContext = oldStatementContext;
    }

    /**
     * prints a continue statement
     */
    public void visitContinueStatement(JContinueStatement self,
                                       String label) {
        newLine();
        print("continue");
        if (label != null) {
            print(" " + label);
        }
        print(";");
    }

    /**
     * prints a break statement
     */
    public void visitBreakStatement(JBreakStatement self,
                                    String label) {
        newLine();
        print("break");
        if (label != null) {
            print(" " + label);
        }
        print(";");
    }


    /**
     * prints a compound statement
     */
    public void visitCompoundStatement(JStatement[] body) {
	boolean oldStatementContext = statementContext;
	statementContext = true;
        for (int i = 0; i < body.length; i++) {
            if (!(body[i] instanceof JEmptyStatement))
		newLine();
            body[i].accept(this);
        }
	statementContext = oldStatementContext;
    }

    /**
     * prints an block statement
     */
    public void visitBlockStatement(JBlock self,
                                    JavaStyleComment[] comments) {
	boolean oldStatementContext = statementContext;
	statementContext = true;
        print("{");
        pos += TAB_SIZE;
        visitCompoundStatement(self.getStatementArray());
        pos -= TAB_SIZE;
        newLine();
        print("}");
	statementContext = oldStatementContext;
    }

    /**
     * prints a type declaration statement
     */
    public void visitTypeDeclarationStatement(JTypeDeclarationStatement self,
                                              JTypeDeclaration decl) {
	
	boolean oldStatementContext = statementContext;
	statementContext = false;
        decl.accept(this);
	statementContext = oldStatementContext;
    }

    // ----------------------------------------------------------------------
    // EXPRESSION
    // ----------------------------------------------------------------------

    /**
     * prints an unary plus expression
     */
    public void visitUnaryPlusExpression(JUnaryExpression self,
                                         JExpression expr)
    {
	print("(");
        print("+");
        expr.accept(this);
	print(")");
    }

    /**
     * prints an unary minus expression
     */
    public void visitUnaryMinusExpression(JUnaryExpression self,
                                          JExpression expr)
    {
	print("(");
        print("-");
        expr.accept(this);
	print(")");
    }

    /**
     * prints a bitwise complement expression
     */
    public void visitBitwiseComplementExpression(JUnaryExpression self,
						 JExpression expr)
    {
	print("(");
        print("~");
        expr.accept(this);
	print(")");
    }

    /**
     * prints a logical complement expression
     */
    public void visitLogicalComplementExpression(JUnaryExpression self,
						 JExpression expr)
    {
	print("(");
        print("!");
        expr.accept(this);
	print(")");
    }

    /**
     * prints a type name expression
     */
    public void visitTypeNameExpression(JTypeNameExpression self,
                                        CType type) {
	print("(");
        print(type);
	print(")");
    }


    /**
     * prints a shift expression
     */
    public void visitShiftExpression(JShiftExpression self,
                                     int oper,
                                     JExpression left,
                                     JExpression right) {
	print("(");
        left.accept(this);
        if (oper == OPE_SL) {
            print(" << ");
        } else if (oper == OPE_SR) {
            print(" >> ");
        } else {
            print(" >>> ");
        }
        right.accept(this);
	print(")");
    }

    /**
     * prints a prefix expression
     */
    public void visitPrefixExpression(JPrefixExpression self,
                                      int oper,
                                      JExpression expr) {
	printLParen();
	boolean oldStatementContext = statementContext;
	statementContext = false;
        if (oper == OPE_PREINC) {
            print("++");
        } else {
            print("--");
        }
        expr.accept(this);
	statementContext = oldStatementContext;
	printRParen();
    }

    /**
     * prints a postfix expression
     */
    public void visitPostfixExpression(JPostfixExpression self,
                                       int oper,
                                       JExpression expr) {
	printLParen();
	boolean oldStatementContext = statementContext;
	statementContext = false;
        expr.accept(this);
        if (oper == OPE_POSTINC) {
            print("++");
        } else {
            print("--");
        }
	statementContext = oldStatementContext;
	printRParen();
    }

    /**
     * prints a parenthesed expression
     */
    public void visitParenthesedExpression(JParenthesedExpression self,
                                           JExpression expr) {
	boolean oldStatementContext = statementContext;
	statementContext = false;
        print("(");
        expr.accept(this);
        print(")");
	statementContext = oldStatementContext;
    }


    /**
     * prints a local variable expression
     */
    public void visitLocalVariableExpression(JLocalVariableExpression self,
                                             String ident) {
        print(ident);
    }
    /**
     * prints an equality expression
     */
    public void visitEqualityExpression(JEqualityExpression self,
                                        boolean equal,
                                        JExpression left,
                                        JExpression right) {
	printLParen();
	boolean oldStatementContext = statementContext;
	statementContext = false;
        left.accept(this);
        print(equal ? " == " : " != ");
        right.accept(this);
	statementContext = oldStatementContext;
	printRParen();
    }

    /**
     * prints a conditional expression
     */
    public void visitConditionalExpression(JConditionalExpression self,
                                           JExpression cond,
                                           JExpression left,
                                           JExpression right) {
	printLParen();
	boolean oldStatementContext = statementContext;
	statementContext = false;
        cond.accept(this);
        print(" ? ");
        left.accept(this);
        print(" : ");
        right.accept(this);
	statementContext = oldStatementContext;
	printRParen();
    }

    /**
     * prints a compound expression
     */
    public void visitCompoundAssignmentExpression(JCompoundAssignmentExpression self,
                                                  int oper,
                                                  JExpression left,
                                                  JExpression right) {
	printLParen();
	boolean oldStatementContext = statementContext;
        left.accept(this);
	statementContext = false;
        switch (oper) {
        case OPE_STAR:
            print(" *= ");
            break;
        case OPE_SLASH:
            print(" /= ");
            break;
        case OPE_PERCENT:
            print(" %= ");
            break;
        case OPE_PLUS:
            print(" += ");
            break;
        case OPE_MINUS:
            print(" -= ");
            break;
        case OPE_SL:
            print(" <<= ");
            break;
        case OPE_SR:
            print(" >>= ");
            break;
        case OPE_BSR:
            print(" >>>= ");
            break;
        case OPE_BAND:
            print(" &= ");
            break;
        case OPE_BXOR:
            print(" ^= ");
            break;
        case OPE_BOR:
            print(" |= ");
            break;
        }
        right.accept(this);
	statementContext = oldStatementContext;
	printRParen();
    }

    /**
     * prints a cast expression
     */
    public void visitCastExpression(JCastExpression self,
				    JExpression expr,
				    CType type)
    {
	printLParen();
	boolean oldStatementContext = statementContext;
	statementContext = false;
	//suppress generation of casts for multidimensional arrays
	//when generating C code because they are meaningless
	//and if we try to access a multi-dim array after it has been cast to 
	//(type **) they dimensions are unknown...
	if (!(type.isArrayType() 
	      && ((CArrayType)type).getElementType().isArrayType())) {
	    print("(");
	    print(type);
	    print(")");
	}
        print("(");
	expr.accept(this);
	print(")");
	statementContext = oldStatementContext;
        printRParen();
    }
    
    /**
     * prints a cast expression
     */
    public void visitUnaryPromoteExpression(JUnaryPromote self,
                                            JExpression expr,
                                            CType type)
    {
	boolean oldStatementContext = statementContext;
        printLParen();
	statementContext = false;
        print("(");
        print(type);
        print(")");
        print("(");
        expr.accept(this);
        print(")");
	statementContext = oldStatementContext;
        printRParen();
    }

    /**
     * Split expression into list of expressions for print.
     * 
     * The C backends are not set up to perform Java-like string 
     * concatenation, and the C++ backends often can not perform
     * string concatenation because of semantic diffrences between
     * the C++ + operator and the Java + operator.
     * 
     * We solve this by looking for all string contatenations reachable 
     * from the root of the expression without going through any operator
     * other than string concatenation and return a List of expressions
     * -- in left-to-right order -- that were connected by string 
     * concatenation in the original expression.
     * 
     * I am following the belief that there are some expressions for which
     * no type can be found.  As per previous implementations, such expressions
     * do not cause an uncaught exception, but they may generate bad code...
     * 
     * @param exp
     * @return
     */
    protected List /*<JExpression>*/ splitForPrint(JExpression exp) {
    	List exprs = new ArrayList(1);
    	if (exp instanceof JAddExpression) {
    		JExpression l, r;
    		CType lt = null; 
    		CType rt = null;
    		
    		l = ((JAddExpression)exp).getLeft();
    		r = ((JAddExpression)exp).getRight();
    		try {
    			lt = l.getType();
    		} catch (Exception e) { /* leave Null if type not recorded */ }
    		try {
    			rt = r.getType();
    		} catch (Exception e) { /* leave Null if type not recorded */ }

    		if ((lt != null && lt.equals(CStdType.String))
    			 || (rt != null && rt.equals(CStdType.String) )) {
    			exprs.addAll(splitForPrint(l));
    			exprs.addAll(splitForPrint(r));
    		} else {
    			exprs.add(exp);
    		}
    	} else {
    		exprs.add(exp);
    	}
    	return exprs;
    }

    protected boolean printExp(JExpression expr) {
	boolean oldStatementContext = statementContext;
	statementContext = true;

    	List exps = splitForPrint(expr);
    	boolean printedOK = true;
    	for (Iterator i = exps.iterator(); i.hasNext();) {
    		JExpression exp = ((JExpression)i.next());
    		CType t = null;
    		try {
    			t = exp.getType();
    		} catch (Exception e) {
    			System.err.println("Cannot get type for print statement");
    			printedOK = false;
    		}
    		String typeString = t.toString();
    		String printPrefix = (String)(printPrefixMap.get(typeString));
    		if (printPrefix == null) {
    			System.err.println("Print statement does not support type "
    					+ t);
    			printedOK = false;
    		} else {
    			print(printPrefix);
    			exp.accept(this);
			String printPostfix = 
			    (String)(printPostfixMap.get(typeString));
			if (printPostfix == null) {
			    printPostfix = ");";
			}
    			print(printPostfix);
    		}
    	}
	statementContext = oldStatementContext;
	return printedOK;
    }
    
    /**
     * Process a Print statment, table driven to allow several backends
     * Deals with the problem of string concatenation in Java not translating
     * to our output languages C or C++
     */
    
   public void visitPrintStatement(SIRPrintStatement self,
            JExpression exp) {
    	printExp(exp);
    	if (self.getNewline()) {
    		print("printf(\"\\n\");\n");
    	}
    }
    
}