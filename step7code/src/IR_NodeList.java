import java.util.LinkedList;
import java.util.HashMap;

public class IR_NodeList{
    public static final String MUL = "*";
    public static final String DIV = "/";
    public static final String ADD = "+";
    public static final String SUB = "-";
    public static final String LPAR = "(";
    public static final String RPAR = ")";

    public static final String LT = "<";
    public static final String GT = ">";
    public static final String EQ = "=";
    public static final String NEQ = "!=";
    public static final String LTE = "<=";
    public static final String GTE = ">=";

    private LinkedList<IR_Node> ir_list;
    private HashMap<String, String> typeDictionary;
    private HashMap<String, String> varDictionary;
    private int regTempCounter;
    private int regParamCounter;
    private int regLocalCounter;

    public IR_NodeList(){
	ir_list = new LinkedList<IR_Node>();

	typeDictionary = new HashMap<String, String>();
	varDictionary = new HashMap<String, String>();
	regTempCounter = 1;
	regParamCounter = 1;
	regLocalCounter = 1;
    }

    public LinkedList<IR_Node> getLinkedList(){
	return ir_list;
    }

    public void append(IR_NodeList otherList){
	ir_list.addAll(otherList.getLinkedList());
    }

    public void setDictionaries(HashMap<String, String> varDict, HashMap<String, String> typeDict){
	varDictionary = new HashMap<String, String>(varDict);
	typeDictionary = new HashMap<String, String>(typeDict);
    }

    public HashMap<String, String> getTypeDictionary(){
	return typeDictionary;
    }

    public HashMap<String, String> getVarDictionary(){
	return varDictionary;
    }

    public void addHeader(){
	ir_list.add(new IR_Node("IR code", "", "", ""));
    }

    public void addFuncHeader(String funcName){
	ir_list.add(new IR_Node("LABEL", "", "", funcName));
	ir_list.add(new IR_Node("LINK", "", "", ""));
    }

    public void print(){
	for(int i = 0; i < ir_list.size(); i++){
	    System.out.println("ir_list[" + i + "]: ");
	    ir_list.get(i).terminalPrint();
	}
    }

    public IR_Node get(int i){
	return ir_list.get(i);
    }

    public void remove(int i){
	ir_list.remove(i);
    }

    public void add(IR_Node node){
	ir_list.add(node);
    }

    public int size(){
	return ir_list.size();
    }

    public void typeDictionaryPut(String key, String value){
	typeDictionary.put(key, value);
    }

    //The lhs and rhs should be evaluated by traverseExpression so we don't need to check if value1 or value2 are literals
    public String createCondIR_Node(String value1, String value2, String compOp, String destination){
	//We dont need to check if either value is a float or an int, but the tiny code does.
	//The register-type dictionary should be up to date with that information.
	//Therefore, we don't need two separate functions for making the IR node
	value1 = replaceValueWithReg(value1);
	value2 = replaceValueWithReg(value2);
	destination = replaceValueWithReg(destination);

	IR_Node node = null;
	destination = "label" + destination;

	switch (compOp){
	case LT:
	    node = new IR_Node("GE", value1, value2, destination);
	    break;
	case GT:
	    node = new IR_Node("LE", value1, value2, destination);
	    break;
	case EQ:
	    node = new IR_Node("NE", value1, value2, destination);
	    break;
	case NEQ:
	    node = new IR_Node("EQ", value1, value2, destination);
	    break;
	case LTE:
	    node = new IR_Node("GT", value1, value2, destination);
	    break;
	case GTE:
	    node = new IR_Node("LT", value1, value2, destination);
	    break;

	default:
	    break;
	}

	//Add to the linked list
	ir_list.add(node);

	return destination;
    }

    public void createLinkIR_Node(int value){
	IR_Node node = new IR_Node("LINK", "", "", "");
	node.setLinkValue(value);
	ir_list.add(node);
    }

    public void createPopIR_Node(String destination){
	IR_Node node = new IR_Node("POP", "", "", destination);

	ir_list.add(node);

    }

    public void createPushIR_Node(String destination){
	IR_Node node = new IR_Node("PUSH", "", "", destination);

	//Add to the linked list
	ir_list.add(node);
    }

    public void createRetIR_Node(){
	IR_Node node = new IR_Node("RET", "", "", "");

	//Add to the linked list
	ir_list.add(node);
    }

    public void createJumpIR_Node(String labelNumber){
	String labelName = "label" + labelNumber;
	IR_Node node = new IR_Node("JUMP", "", "", labelName);

	//Add to the linked list
	ir_list.add(node);

    }

    public void createJsrIR_Node(String routine){
	IR_Node node = new IR_Node("JSR", "", "", routine);
	
	//Add to the linked list
	ir_list.add(node);
    }

    public void createLabelIR_Node(String labelNumber){
	String labelName = "label" + labelNumber;
	IR_Node node = new IR_Node("LABEL", "", "", labelName);
	
	//Add to the linked list
	ir_list.add(node);

    }

    //Note: Each time you create an IR_Node you just pass the destination reg w/ the value of currentReg.
    //The IR_Node creation automatically increments the currentReg.
    public String createOpIR_Node(String value1, String value2, String op, String destination){
	value1 = replaceValueWithReg(value1);
	value2 = replaceValueWithReg(value2);
	destination = replaceValueWithReg(destination);

	String val1Type = checkValType(value1);
	String val2Type = checkValType(value2);
	boolean isVal1Literal = false;
	boolean isVal2Literal = false;

	//Check if either value is a literal and if so store it to a temporary register first.
	if(val1Type.equals("FLOATLITERAL") || val1Type.equals("INTLITERAL")){
	    createStoreIR_Node(value1, destination);
	    value1 = destination;//Set to the destination register for the opcode.
	    isVal1Literal = true;
	}
	
	if(val2Type.equals("FLOATLITERAL") || val2Type.equals("INTLITERAL")){
	    if(isVal1Literal){
		destination = getCurrentTempRegister();
	    }
	    createStoreIR_Node(value2, destination);
	    value2 = destination;
	    isVal2Literal = true;
	}

	if(isVal1Literal || isVal2Literal){//Update the destination if either value was a literal
	    destination = getCurrentTempRegister();
	}
	if(val1Type.equals("FLOAT") || val2Type.equals("FLOAT") || val1Type.equals("FLOATLITERAL") || val2Type.equals("FLOATLITERAL")){
	    //value1 and value2 are preset to their destination register if they are literals
	    createFloatOpIR_Node(value1, value2, op, destination);
	} else {
	    createIntOpIR_Node(value1, value2, op, destination);
	}

	return destination;
    }


    private void createIntOpIR_Node(String value1, String value2, String op, String destination){
	IR_Node node = null;

	switch(op){
	case MUL:
	    node = new IR_Node("MULTI", value1, value2, destination);
	    break;
	    
	case DIV:
	    node = new IR_Node("DIVI", value1, value2, destination);
	    break;

	case ADD:
	    node = new IR_Node("ADDI", value1, value2, destination);
	    break;

	case SUB:
	    node = new IR_Node("SUBI", value1, value2, destination);
	    break;

	default: 
	    break;
	    
	}
	
	//Add to the linked list
	ir_list.add(node);

	//Add to the dictionary
	addRegToTypeDictionary(destination, "INT");//This is where regTempCounter is incremented.
    }

    private void createFloatOpIR_Node(String value1, String value2, String op, String destination){
	IR_Node node = null;

	switch(op){
	case MUL:
	    node = new IR_Node("MULTF", value1, value2, destination);
	    break;
	    
	case DIV:
	    node = new IR_Node("DIVF", value1, value2, destination);
	    break;

	case ADD:
	    node = new IR_Node("ADDF", value1, value2, destination);
	    break;

	case SUB:
	    node = new IR_Node("SUBF", value1, value2, destination);
	    break;

	default:
	    break;
	    
	}

	//Add to the linked list
	ir_list.add(node);

	//Add to the dictionary
	addRegToTypeDictionary(destination, "FLOAT");
    }

    public void createStoreIR_Node(String value, String destination){
	value = replaceValueWithReg(value);
	destination = replaceValueWithReg(destination);

	//System.out.println("Store: " + value + " ---> " + destination);

	String type = checkValType(value);
	
	if(type.equals("INT") || type.equals("INTLITERAL")){
	    IR_Node node = new IR_Node("STOREI", value, "", destination);
	    //Add to the linked list
	    ir_list.add(node);
	    //Add to the dictionary
	    addRegToTypeDictionary(destination, "INT");
	} else {
	    IR_Node node = new IR_Node("STOREF", value, "", destination);
	    //Add to the linked list
	    ir_list.add(node);
	    //Add to the dictionary
	    addRegToTypeDictionary(destination, "FLOAT");
	}

    }

    //returns the original value or the type of or register associated with it
    public String replaceValueWithReg(String value){
	String regNum = varDictionary.get(value);

	if(regNum == null){
	    return value;
	} else{
	    return regNum;
	}
	
    }

    public void createReadIR_Node(String value){
	value = replaceValueWithReg(value);
	String type = checkValType(value);

	if(type.equals("STRING")){
	    IR_Node node = new IR_Node("READS", value, "", "");
	    //Add to the linked list
	    ir_list.add(node);
	} else if(type.equals("INT") || type.equals("INTLITERAL")){
	    IR_Node node = new IR_Node("READI", value, "", "");
	    //Add to the linked list
	    ir_list.add(node);
	} else {
	    IR_Node node = new IR_Node("READF", value, "", "");
	    //Add to the linked list
	    ir_list.add(node);
	}	
    }

    public void createWriteIR_Node(String value){
	value = replaceValueWithReg(value);
	String type = checkValType(value);

	if(type.equals("STRING")){
	    IR_Node node = new IR_Node("WRITES", value, "", "");
	    //Add to the linked list
	    ir_list.add(node);
	} else if(type.equals("INT") || type.equals("INTLITERAL")){
	    IR_Node node = new IR_Node("WRITEI", value, "", "");
	    //Add to the linked list
	    ir_list.add(node);
	} else {
	    IR_Node node = new IR_Node("WRITEF", value, "", "");
	    //Add to the linked list
	    ir_list.add(node);
	}

    }

    public void addToDictionaries(String varName, String regName, String type){
	varDictionary.put(varName, regName);
	typeDictionary.put(regName, type);

	if(regName.startsWith("$T")){
	    regTempCounter++;
	} else if(regName.startsWith("$P")){
	    regParamCounter++;
	} else if(regName.startsWith("$L")){
	    regLocalCounter++;
	}
    }

    public void addRegToTypeDictionary(String value, String type){
	//Check if value is a reg
	boolean isReg = false;
	if(String.valueOf(value.charAt(0)).equals("$")){//destination is a register
	    isReg = true;
	} else{
	    return;
	}

	//Add to dictionay
	//Note that it will overwrite existing keys.
	typeDictionary.put(value, type);

	//Register Count is incremented
	if(value.startsWith("$T")){
	    regTempCounter++;
	} /*else if(value.startsWith("$P")){
	    regParamCounter++;
	} else if(value.startsWith("$L")){
	    regLocalCounter++;
	    }*/
    }

    public String checkValType(String value){
	String type = typeDictionary.get(value);

	if(type == null){
	    if(value.matches("[0-9]*\\.[0-9]+")){
		return "FLOATLITERAL";
	    } else if(value.matches("[0-9]+")){
		return "INTLITERAL";
	    } /*else if(String.valueOf(value.charAt(0)).equals("$")){
		return typeDictionary.get(value);//Lookup the type for the register
		} */
	    else {
		type = typeDictionary.get(varDictionary.get(value));

		if(type == null){
		    System.out.println("ERROR: Value: " + value + "\nNot found to have a type!!!");
		}
		return type;
	    }
	} else{
	    return type;
	}
	
    }

    //Add different getReg functions for params, temp, and local regs
    //Adjust node creation method so that the appropriate reg counter is incremented.
    //When the user sets the destination and opcode parameters they will choose which type of register to use.
    public String getCurrentTempRegister(){
	return "$T" + regTempCounter;
    }
    
    public String getCurrentParamRegister(){
	return "$P" + regParamCounter;
    }

    public String getCurrentLocalRegister(){
	return "$L" + regLocalCounter;
    }

}
