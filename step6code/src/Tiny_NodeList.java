import java.util.LinkedList;

import java.io.PrintWriter;

class Tiny_NodeList{
    private LinkedList<Tiny_Node> tiny_rep;
    private IR_NodeList inter_rep;
    private int largestTempRegUsed;

    public Tiny_NodeList(){
	tiny_rep = new LinkedList<Tiny_Node>();
	inter_rep = null;
	largestTempRegUsed = -1;
    }

    public Tiny_NodeList(IR_NodeList nodeList){
	tiny_rep = new LinkedList<Tiny_Node>();

	inter_rep = nodeList;
	convertToTinyList(nodeList);
    }

    public Tiny_NodeList(IR_NodeList nodeList, LinkedList<Tiny_Node> varDecl){
	tiny_rep = new LinkedList<Tiny_Node>();

	tiny_rep.add(new Tiny_Node(";tiny code", "", ""));
	for(int i = 0; i < varDecl.size(); i++){
	    tiny_rep.add(varDecl.get(i));
	}
	
	createPushNode("");
	createPushNode("r0");
	createPushNode("r1");
	createPushNode("r2");
	createPushNode("r3");	
	Tiny_Node node = new Tiny_Node("jsr", "main", "" );
	tiny_rep.add(node);
	createHaltNode();

	nodeList.remove(0);//Removes the ";IR Code" header
	inter_rep = nodeList;
	convertToTinyList(nodeList);
    }

    public LinkedList<Tiny_Node> getTiny_rep(){
	return tiny_rep;
    }

    public void append(Tiny_NodeList otherList){
	tiny_rep.addAll(otherList.getTiny_rep());
    }

    public void printTinyList(PrintWriter writer){
	for(int i = 0; i < tiny_rep.size(); i++){
	    Tiny_Node node = tiny_rep.get(i);
	    node.print(writer);
	}
    }
    
    //Needs to be reworked w/ the constructor
    public void convertToTinyList(IR_NodeList IR_List){
	for(int i = 0; i < IR_List.size(); i++){
	    convertToTinyNode(IR_List.get(i));
	}

	//The end of the current IR_list is not necessarily the end of all the code.
	//tiny_rep.add(new Tiny_Node("sys halt", "", ""));

    }
    
    /*
     * This function converts an IRnode parameters in to a tinyNode syntax.
     * It also automatically adds the created tinyNode to the tiny_rep
     */
    public void convertToTinyNode(IR_Node IRnode){
	String opcode = IRnode.getOperation();

	//Convert the node parameters to tiny code syntax
	String tinyOp1 = convertToTinyOperand(IRnode.getOperand1());
	String tinyOp2 = convertToTinyOperand(IRnode.getOperand2());
	String tinyDest = convertToTinyOperand(IRnode.getDestination());
	
	//Check if the current opcode is for a conditional or unconditional jump, or a label
	//For the cases where the opType doesn't matter
	boolean typeMatters = false;

	switch(opcode){
	case "LABEL":
	    createLabelNode(tinyDest);
	    break;

	case "RET":
	    createLinkNode(-1, false);
	    createRetNode();
	    break;

	case "LINK":
	    String reg = inter_rep.getCurrentLocalRegister();
	    int regNum = Integer.parseInt(reg.substring(2, reg.length())) - 1;

	    createLinkNode(regNum, true);
	    break;

	case "PUSH":
	    opcode = "push";
	    createPushNode(tinyDest);
	    break;

	case "POP":
	    opcode = "pop";
	    createPopNode(tinyDest);
	    break;

	case "JSR":
	    opcode = "jsr";
	    createJumpNode(opcode, tinyDest);
	    break;

	case "JUMP":
	    opcode = "jmp";
	    createJumpNode(opcode, tinyDest);
	    break;

	default:
	    typeMatters = true;
	    break;
	}

	//Function terminates if opcode type didn't matter
	if(!typeMatters){
	    return;
	}

	boolean isCond = true;
	//Evaluate if its a conditional operation
	switch(opcode){
	case "LT":
	    opcode = "jlt";
	    createCmpNode(IRnode.getOperand1(), IRnode.getOperand2());
	    createJumpNode(opcode, tinyDest);
	    break;

	case "GT":
	    opcode = "jgt";
	    createCmpNode(IRnode.getOperand1(), IRnode.getOperand2());
	    createJumpNode(opcode, tinyDest);
	    break;

	case "EQ":
	    opcode = "jeq";
	    createCmpNode(IRnode.getOperand1(), IRnode.getOperand2());
	    createJumpNode(opcode, tinyDest);
	    break;

	case "LE":
	    opcode = "jle";
	    createCmpNode(IRnode.getOperand1(), IRnode.getOperand2());
	    createJumpNode(opcode, tinyDest);
	    break;

	case "GE":
	    opcode = "jge";
	    createCmpNode(IRnode.getOperand1(), IRnode.getOperand2());
	    createJumpNode(opcode, tinyDest);
	    break;

	case "NE":
	    opcode = "jne";
	    createCmpNode(IRnode.getOperand1(), IRnode.getOperand2());
	    createJumpNode(opcode, tinyDest);
	    break;

	default:
	    isCond = false;
	    break;
	}

	//Terminating point if the function was a conditional
	if(isCond){
	    return;
	}

	//Only do this if they opcode is an arithmetic operation, write, read, or store
	String tinyOpcode = convertToTinyOpcode(opcode);

	String operation = tinyOpcode.substring(0, tinyOpcode.length()-1);
	switch(operation){
	case "sys write":
	    createWriteNode(tinyOpcode, tinyOp1);
	    break;
		
	case "sys read":
	    createReadNode(tinyOpcode, tinyOp1);
	    break;

	case "move":
	    createMoveNode(tinyOp1, tinyDest);
	    break;
		
	default:
	    createOpNode(tinyOpcode, tinyOp1, tinyOp2, tinyDest);
	    break;
	}

    }

    //This function can receive the parameters in tiny syntax or IR syntax
    public void createOpNode(String opcode, String op1, String op2, String dest){
	//Note: For efficient register use, there should be a conditional to determine whether a move is required.
	//move operand1 dest
	createMoveNode(op1, dest);

	//(operator) operand2 dest
	Tiny_Node node = new Tiny_Node(opcode, op2, dest);
	tiny_rep.add(node);
    }

    public void createMoveNode(String op1, String dest){
	//This should only be used for returns otherwise we may get register overlap
	if(op1.startsWith("$") && dest.startsWith("$")){
	    String intermediateReg = "r" + (largestTempRegUsed + 1);
	    createMoveNode(op1, intermediateReg);

	    op1 = intermediateReg;
	}

	Tiny_Node node = new Tiny_Node("move", op1, dest);
	tiny_rep.add(node);

    }

    public void createReadNode(String opcode, String op1){
	Tiny_Node node = new Tiny_Node(opcode, op1, "");
	tiny_rep.add(node);
    }

    public void createWriteNode(String opcode, String op1){
	Tiny_Node node = new Tiny_Node(opcode, op1, "");
	tiny_rep.add(node);
    }

    public void createCmpNode(String IR_op1, String IR_op2){
	//Determine types for op1 and op2 and set i or r for the opcode
	String val1Type = inter_rep.checkValType(IR_op1);
	String val2Type = inter_rep.checkValType(IR_op2);
	String opcode;

	if(val1Type.equals("FLOAT") || val2Type.equals("FLOAT") || val1Type.equals("FLOATLITERAL") || val2Type.equals("FLOATLITERAL")){
	    opcode = "cmpr";
	} else{
	    opcode = "cmpi";
	}

	String tinyOp1 = convertToTinyOperand(IR_op1);
	String tinyOp2 = convertToTinyOperand(IR_op2);

	//Use the most recent temp r# to avoid passing two mem locations
	if(tinyOp2.startsWith("$")){
	    String intermediateReg = "r" + (largestTempRegUsed + 1);
	    createMoveNode(tinyOp2, intermediateReg);

	    tinyOp2 = intermediateReg;
	}




	//Create the cmp node
	Tiny_Node node = new Tiny_Node(opcode, tinyOp1, tinyOp2);
	tiny_rep.add(node);
    }

    public void createHaltNode(){
	Tiny_Node node = new Tiny_Node("sys halt", "", "" );
	tiny_rep.add(node);	
    }

    public void createRetNode(){
	Tiny_Node node = new Tiny_Node("ret", "", "" );
	tiny_rep.add(node);	
    }

    public void createPushNode(String target){
	Tiny_Node node = new Tiny_Node("push", target, "" );
	tiny_rep.add(node);	
    }

    public void createPopNode(String target){
	Tiny_Node node = new Tiny_Node("pop", target, "" );
	tiny_rep.add(node);	
    }

    public void createLinkNode(int value, boolean link){
	Tiny_Node node;
	if(link){
	    node = new Tiny_Node("link", Integer.toString(value), "" );
	} else{//value int is ignored if unlnk
	    node = new Tiny_Node("unlnk", "", "" );
	}
	tiny_rep.add(node);	
    }

    public void createJumpNode(String opcode, String target){
	if(opcode.equals("jsr")){
		createPushNode("r0");
		createPushNode("r1");
		createPushNode("r2");
		createPushNode("r3");	
	}

	Tiny_Node node = new Tiny_Node(opcode, target, "" );
	tiny_rep.add(node);

	if(opcode.equals("jsr")){
		createPopNode("r3");
		createPopNode("r2");
		createPopNode("r1");
		createPopNode("r0");	
	}
    }

    public void createLabelNode(String labelName){
	Tiny_Node node = new Tiny_Node("label", labelName, "");
	tiny_rep.add(node);
    }

    //This function converts IR operands to Tiny operands.
    public String convertToTinyOperand(String IR_Operand){
	if(IR_Operand.startsWith("$T")){
	    int regNum = Integer.parseInt(IR_Operand.substring(2)) - 1;

	    if(regNum > largestTempRegUsed){
		largestTempRegUsed = regNum;
	    }

	    return "r" + regNum;

	} else if(IR_Operand.startsWith("$L")){
	    return IR_Operand.replaceAll("L", "-");

	} else if(IR_Operand.startsWith("$P")){
	    int offset = 5;
	    int regNum = Integer.parseInt(IR_Operand.substring(2)) + offset;
	    return "$" + Integer.toString(regNum);
	    
	} else if(IR_Operand.startsWith("$R")){
	    int offset = 5;
	    int regNum = Integer.parseInt(inter_rep.getCurrentParamRegister().substring(2)) + offset;
	    
	    return "$" + Integer.toString(regNum);

	}
	

	return IR_Operand;
    }

    //WARNING: Operations ending not in I or F will have issues
    //Automatically creates the tiny nodes if they are not type dependent, ending in I or F
    public String convertToTinyOpcode(String IR_Opcode){
	//opcode is the tiny opcode
	String opcode = IR_Opcode.toLowerCase();

	String opType = opcode.substring(opcode.length()-1);
	if(opType.equals("f")){
	    opType = "r";
	} else if(opType.equals("s")){
	    opType = "s";
	} else {
	    opType = "i";
	}

	opcode = opcode.substring(0, opcode.length()-1);//grabs everything but last char
	

	switch(opcode){
	case "mult":
	    opcode = "mul" + opType;
	    break;

	case "write":
	    opcode = "sys write" + opType;
	    break;

	case "read":
	    opcode = "sys read" + opType;
	    break;

	case "store":
	    opcode = "move" + opType;
	    break;

	default:
	    opcode = opcode + opType;
	    break;
	}

	return opcode;
    }

}
