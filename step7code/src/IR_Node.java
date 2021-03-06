import java.io.*;
import java.io.PrintWriter;

public class IR_Node{

    private String opcode;
    private String op1;
    private String op2;
    private String destination;

    private int linkValue;

    public IR_Node(String operation, String operand1, String operand2, String dest){
	opcode = operation;
	op1 = operand1;
	op2 = operand2;
	destination = dest;

	linkValue = 0;
    }

    public int getLinkValue(){
	return linkValue;
    }

    public void setLinkValue(int value){
	linkValue = value;
    }

    public void terminalPrint(){
	System.out.println(opcode + " " + op1 + " " + op2 + " " + destination);
    }
    
    public String getOperation(){
	return opcode;
    }

    public String getOperand1(){
	return op1;
    }

    public String getOperand2(){
	return op2;
    }

    public String getDestination(){
	return destination;
    }

    public void print(PrintWriter writer){
	//The case is to prevent extra whitespace
	String temp = opcode.substring(0, opcode.length()-1);

	switch(temp){
	case "STORE":
	    writer.println(";" + opcode + " " + op1 + " " + destination);
	    break;
	    
	case "WRITE":
	    writer.println(";" + opcode + " " + op1);
	    break;

	case "READ":
	    writer.println(";" + opcode + " " + op1 + " " + destination);
	    break;

	case "JUM":
	    writer.println(";" + opcode + " " + destination);
	    break;

	case "LABE":
	    writer.println(";" + opcode + " " + destination);
	    break;

	default:
	    writer.println(";" + opcode + " " + op1 + " " + op2 + " " + destination);
	    break;
	}

    }
}
