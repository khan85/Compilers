import java.util.LinkedList;
import java.lang.Integer;
import java.io.PrintWriter;

class Tiny_NodeList{
    private LinkedList<Tiny_Node> tiny_rep;

    public Tiny_NodeList(LinkedList<IR_Node> nodeList, LinkedList<Tiny_Node> varDecl){
	tiny_rep = new LinkedList<Tiny_Node>();

	tiny_rep.add(new Tiny_Node(";tiny code", "", ""));
	for(int i = 0; i < varDecl.size(); i++){
	    tiny_rep.add(varDecl.get(i));
	}
	
	nodeList.remove(0);
	convertToTinyList(nodeList);
    }

    public void printTinyList(PrintWriter writer){
	for(int i = 0; i < tiny_rep.size(); i++){
	    Tiny_Node node = tiny_rep.get(i);
	    node.print(writer);
	}
    }
    
    //Needs to be reworked w/ the constructor
    public void convertToTinyList(LinkedList<IR_Node> IR_List){
	for(int i = 0; i < IR_List.size(); i++){
	    convertToTinyNode(IR_List.get(i));
	}

	tiny_rep.add(new Tiny_Node("sys halt", "", ""));

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
	String tinyOpcode = convertToTinyOpcode(opcode);

	String operation = tinyOpcode.substring(0, tinyOpcode.length()-1);
	switch(operation){
	case "sys write":
	    createWriteNode(tinyOpcode, tinyOp1);
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
	Tiny_Node node = new Tiny_Node("move", op1, dest);
	tiny_rep.add(node);

    }

    public void createWriteNode(String opcode, String op1){
	Tiny_Node node = new Tiny_Node(opcode, op1, "");
	tiny_rep.add(node);
    }

    //This function converts IR operands to Tiny operands.
    public String convertToTinyOperand(String IR_Operand){
	if(IR_Operand.startsWith("$T")){
	    int regNum = Integer.parseInt(IR_Operand.substring(2)) - 1;
	    return "r" + regNum;
	}

	return IR_Operand;
    }

    //WARNING: Operations ending not in I or F will have issues
    public String convertToTinyOpcode(String IR_Opcode){
	//opcode is the tiny opcode
	String opcode = IR_Opcode.toLowerCase();
	String opType = opcode.substring(opcode.length()-1);
	opType = opType.equals("f") ?  "r" : "i";//WARNING

	opcode = opcode.substring(0, opcode.length()-1);//grabs everything but last char

	switch(opcode){
	case "mult":
	    opcode = "mul" + opType;
	    break;

	case "write":
	    opcode = "sys write" + opType;
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
