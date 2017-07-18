import java.util.Stack;
import java.util.Arrays;

import com.sun.xml.internal.ws.util.StringUtils;

public class CodeGeneratoe {

	String registerNames[];
	int registerValues[];
	int wp;

	StringBuilder code;
	// int numberOflines;

	public CodeGeneratoe(int wp) {

		// rule 1: r3 is always a temp reg
		// rule 2: wp is always being added by 4 so reges which modulo to 4 is 3
		// are always temp regs
		// rule 3: memory 1061 1062 are temp
		registerNames = new String[64];
		registerValues = new int[64];
		registerNames[61] = "res0"; // reserved for calculations 1061 in memory
		registerNames[62] = "res1";
		registerNames[63] = "res2";

		this.wp = wp;

		code = new StringBuilder();
		// numberOflines = 0;
	}

	int getNumberOfLines() {

		return code.toString().split("\n").length;
	}

	/**
	 * set name of register[regNum] to varName
	 * 
	 * @param regNum
	 * @param varName
	 */
	void setReg(int regNum, String varName) {

		registerNames[regNum] = varName;
		System.out.println("register " + regNum + " set for " + varName);
	}

	/**
	 * find number of register which name is equel to id
	 * 
	 * @param id
	 * @return
	 */
	int getRegNumber(String id) {

		for (int i = 0; i < 64; i++) {
			if (registerNames[i] != null && registerNames[i].equals(id)) {
				return i;

			}
		}
		return -1;
	}

	/**
	 * find an empty reg and return its number
	 * 
	 * @return
	 */
	int getEmptyReg() {

		for (int i = 0; i < 64; i++) {
			if (registerNames[i] == null) {
				if (i % 4 != 3) {
					return i;
				}
			}
		}

		return -1;
	}

	/**
	 * finding an empty reg and name it to id return number of reg
	 * 
	 * @param id
	 * @return
	 */
	int variableDec(String id) {
		int i = getEmptyReg();

		if (i == -1) {
			System.out.println("no register found");
		}

		setReg(i, id);
		return i;
	}

	int variableDec(String id, int x) {
		int i = getEmptyReg();

		if (i == -1) {
			System.out.println("no register found");
		}

		setReg(i, id);
		registerValues[i] = x;
		setWP(i);
		int offset = i - wp;
		code.append("mil r" + offset + " " + x + "\n");
		code.append("mih r" + offset + " " + x + "\n");

		return i;
	}

	void setWP(int x) {

		// System.out.println(wp+" "+x);
		//code.append(x/4+" "+wp/4+"\n");
		if(wp/4 == x/4){
			return;
		}
		int t = x - (x % 4);
		int i = t - wp;
		
		if (i < 0) {
			code.append("cwp\n");
			wp = 0;
			setWP(x);
			return;
		}
		if (wp != t) {
			wp = t;

			code.append("awp " + i + "\n");
		}

	}

	void calculateExpression(int a, int b, int c, String operand) { // three
																	// registers
																	// taht we
		// want to perfrom operation
		// on them

		int maxi = a > b && a > c ? a : b > c ? b : c;
		int mini = a < b && a < c ? a : b < c ? b : c;

		if (a / 4 != b / 4 || b / 4 != c / 4) {

			setWP(b);
			int offset = b - wp;
			code.append("mil r3 1061\n");
			code.append("mih r3 1061\n");
			code.append("sta r3 r" + offset + "\n");

			setWP(c);
			offset = c - wp;
			code.append("mil r3 1062\n");
			code.append("mih r3 1062\n");
			code.append("sta r3 r" + offset + "\n");

			setWP(61);
			code.append("mil r3 1061\n");
			code.append("mih r3 1061\n");
			code.append("lda r3 r1\n");

			code.append("mil r3 1062\n");
			code.append("mih r3 1062\n");
			code.append("lda r3 r2\n");

			if (operand.equals("+")) {
				code.append("add r2 r1\n");
			} else if (operand.equals("*")) {
				code.append("mul r2 r1\n");
			} else if (operand.equals("-")) {
				code.append("sub r2 r1\n");
			} else if (operand.equals("/")) {
				code.append("div r2 r1\n");
			}

			code.append("sta r3 r2\n");

			setWP(a);
			offset = a - wp;
			code.append("mil r3 1062\n");
			code.append("mih r3 1062\n");
			code.append("lda r" + offset + " r3\n"); // a<=mem[1062]
		} else {
			setWP(mini);
			// System.out.println(c);
			int offset = b - wp;
			code.append("mvr r3 r" + offset + "\n");

			offset = c - wp;
			if (operand.equals("+")) {
				code.append("add r3 r" + offset + "\n");
			} else if (operand.equals("*")) {
				code.append("mul r3 r" + offset + "\n");
			}
			else if(operand.equals("-")){
				code.append("sub r3 r" + offset + "\n");
			}
			else if (operand.equals("/")) {
				code.append("div r3 r" + offset + "\n");
			}

			offset = a - wp;
			code.append("mvr r" + offset + " r3\n");
		}

	}

	public Token[] tokenize(String code) {

		String arr[] = code.split(" ");
		Token ans[] = new Token[arr.length];
		int i = 0;
		for (String str : arr) {
			String type = null;

			if (str.equals("int")) {
				type = "keyword";
			} else if (!str.equals(";")) {
				type = "id";
			}

			Token t = new Token(type, str);
			ans[i] = t;
			i++;
		}
		return ans;
	}

	int calculatePostfix(String postfix[]) {

		Stack<RegInt> s = new Stack<>();
		for (String str : postfix) {

			if (isNumeric(str)) {
				//code.append(str + " is numeric\n");
				RegInt ri = new RegInt(Integer.parseInt(str), false);
				ri.giveReg(this);
				s.push(ri);
			}

			else if (notOperand(str)) {

				int regnum = getRegNumber(str);
				int tmp = variableDec("tmp");
				move(regnum, tmp);
				s.push(new RegInt(tmp, true));
			}

			else {

				RegInt x, y;
				x = s.pop();
				y = s.pop();

				if (!y.isreg) {
					y.giveReg(this);
				}

				if (!x.isreg) {
					x.giveReg(this);
				}
				//System.out.println(str);
				calculateExpression(x.reg, x.reg, y.reg, str);

				if (str.equals("+")) {
					x.value = x.value + y.value; // or any op
				} else if (str.equals("-")) {
					x.value = x.value - y.value; // or any op
				}
				if (str.equals("*")) {
					x.value = x.value * y.value; // or any op
				}
				if (str.equals("/")) {
					x.value = x.value / y.value; // or any op
				}

				registerNames[x.reg] = Integer.toString(x.value);
				registerValues[x.reg] = x.value;
				y.setFree(this);
				s.push(x);
			}
		}
		return s.peek().reg;
	}

	void move(int src, int des) {

		// int diff = src - des > 0 ? src - des : des - src;
		int mini = src < des ? src : des;
		if (src / 4 != des / 4) {

			setWP(src);
			code.append("mil r3 1061\n");
			code.append("mih r3 1061\n");
			int offset = src - wp;
			code.append("sta r3 r" + offset + "\n");

			setWP(des);
			code.append("mil r3 1061\n");
			code.append("mih r3 1061\n");
			offset = des - wp;
			code.append("lda r" + offset + " r3\n");

		} else {
			setWP(mini);
			// System.out.println(mini);
			int offset0 = des - wp;
			int offset1 = src - wp;

			code.append("mvr r" + offset0 + " r" + offset1 + "\n");
		}
	}

	public String[] toPostfix(String infix[]) {

		String ans[] = new String[infix.length];
		int i = 0;
		Stack<String> operands = new Stack<>();
		for (String str : infix) {

			if (notOperand(str)) {
				ans[i] = str;
				i++;
			} else {

				while (!operands.isEmpty() && !isHigher(str, operands.peek())) {
					ans[i] = operands.pop();
					i++;
				}

				operands.push(str);
			}
		}

		while (!operands.isEmpty()) {
			ans[i] = operands.pop();
			i++;
		}

		return ans;
	}

	public boolean isHigher(String s1, String s2) { // s1 is higher than s2

		if (s1.equals("*")) {
			return true;
		}
		return false;
	}

	public static boolean isNumeric(String str) {
		try {
			double d = Double.parseDouble(str);
		} catch (NumberFormatException nfe) {
			return false;
		}
		return true;
	}

	public static boolean notOperand(String str) {

		if (str.equals("+") || str.equals("-") || str.equals("*") || str.equals("/")) {
			return false;
		}

		return true;
	}

	public void generateIf(String[] lines, int type) { // lines should be if if
														// lines
		code.append("before if\n");
		String line = lines[0];
		String condition = line.substring(5); // if (
		String arr[] = condition.split("==");
		int a = calculatePostfix(toPostfix(arr[0].substring(0, arr[0].length()).split(" ")));
		//code.append("before calculating postfix 5\n");
		String arr1[] = toPostfix(arr[1].substring(1, arr[1].length() - 1).split(" "));
	
		int b = calculatePostfix(arr1);
		//code.append("after calculating postfix 5\n");

		//if (a / 4 != b / 4) {

			setWP(a);
			move(b, wp + 3);
			int offset = a - wp;
			code.append("cmp r3 r" + offset + "\n");
			code.append("brz 1\n");
			CodeGeneratoe cg = new CodeGeneratoe(wp);
			cg.registerNames = registerNames;
			cg.registerValues = registerValues;
			cg.generateCode(Arrays.copyOfRange(lines, 1, lines.length));
			//System.out.println("inside if code is\n" + cg.code.toString());
			//System.out.println("finish");
			code.append("jpr " + cg.getNumberOfLines() + "\n");
			code.append(cg.code);
			//code.append("finish\n");
		//}

	}

	void generateCode(String lines[]) {

		for (int j = 0; j < lines.length; j++) {
			 //System.out.println(lines[j]);
			Token tokens[] = tokenize(lines[j]);

			if (tokens[0].type.equals("keyword") && tokens[1].type.equals("id") && tokens[2].value.equals(";")) {

				variableDec(tokens[1].value);
			}

			if (tokens[0].type.equals("keyword") && tokens[1].type.equals("id") && tokens[2].value.equals(",")) {

				variableDec(tokens[1].value);
				int i = 2;
				while (tokens[i].value.equals(",")) {
					i++;
					variableDec(tokens[i].value);
					i++;
				}
			}

			if (tokens[0].type.equals("keyword") && tokens[1].type.equals("id") && tokens[2].value.equals("=")) {

				String expression[] = new String[tokens.length - 3];
				for (int i = 0; i < expression.length; i++) {
					expression[i] = tokens[i + 3].value;
				}

				int ansReg = calculatePostfix(toPostfix(expression));
				int des = variableDec(tokens[1].value);
				// if (des < 0) {
				// des = cg.variableDec(tokens[1].value);
				// }'
				move(ansReg, des);
			}

			if (tokens[0].type.equals("id") && tokens[1].value.equals("=")) {

				String expression[] = new String[tokens.length - 2];
				for (int i = 0; i < expression.length; i++) {
					expression[i] = tokens[i + 2].value;
				}

				int ansReg = calculatePostfix(toPostfix(expression));
				int des = getRegNumber(tokens[0].value);
				if (des < 0) {
					des = variableDec(tokens[0].value);
				}
				move(ansReg, des);
			}

			if (tokens[0].value.equals("if")) {
				//System.out.println("han");
				generateIf(Arrays.copyOfRange(lines, j, j + 2), 0);
				j++;
			}
			
		}
	}

	public static void main(String[] args) {

		String ccode = "int a = 1;if ( a == 10 );a = a + 1";
		//String ccode = "int a = 10 - 7";
		CodeGeneratoe cg = new CodeGeneratoe(0);

		// read code from file
		// split code line by line
		// for each line split the code into tokens
		String lines[] = ccode.split(";");
		cg.generateCode(lines);
		// for (String line : lines) {
		// // System.out.println(line);
		// Token tokens[] = cg.tokenize(line);
		//
		// if (tokens[0].type.equals("keyword") && tokens[1].type.equals("id")
		// && tokens[2].value.equals(";")) {
		//
		// cg.variableDec(tokens[1].value);
		// }
		//
		// if (tokens[0].type.equals("keyword") && tokens[1].type.equals("id")
		// && tokens[2].value.equals(",")) {
		//
		// cg.variableDec(tokens[1].value);
		// int i = 2;
		// while (tokens[i].value.equals(",")) {
		// i++;
		// cg.variableDec(tokens[i].value);
		// i++;
		// }
		// }
		//
		// if (tokens[0].type.equals("keyword") && tokens[1].type.equals("id")
		// && tokens[2].value.equals("=")) {
		//
		// String expression[] = new String[tokens.length - 3];
		// for (int i = 0; i < expression.length; i++) {
		// expression[i] = tokens[i + 3].value;
		// }
		//
		// int ansReg = cg.calculatePostfix(cg.toPostfix(expression));
		// int des = cg.variableDec(tokens[1].value);
		// // if (des < 0) {
		// // des = cg.variableDec(tokens[1].value);
		// // }'
		// cg.move(ansReg, des);
		// }
		//
		// if (tokens[0].type.equals("id") && tokens[1].value.equals("=")) {
		//
		// String expression[] = new String[tokens.length - 2];
		// for (int i = 0; i < expression.length; i++) {
		// expression[i] = tokens[i + 2].value;
		// }
		//
		// int ansReg = cg.calculatePostfix(cg.toPostfix(expression));
		// int des = cg.getRegNumber(tokens[0].value);
		// // if (des < 0) {
		// // des = cg.variableDec(tokens[0].value);
		// // }
		// cg.move(ansReg, des);
		// }
		// cg.code.append("\n");
		// }
		System.out.println(cg.code.toString());

	}

}

class RegInt {

	int value;
	int reg;
	boolean isreg;

	public RegInt(int x, boolean isreg) {
		// TODO Auto-generated constructor stub
		this.isreg = isreg;
		if (isreg) {
			reg = x;
		} else {
			value = x;
		}
	}

	void giveReg(CodeGeneratoe cg) {

//		int tmp = cg.getRegNumber(Integer.toString(value));
//		if (tmp < 0) {
//			tmp = cg.variableDec(Integer.toString(value), value);
//		}

		reg = cg.variableDec(Integer.toString(value), value);

		isreg = true;
	}

	void setFree(CodeGeneratoe cg) {
		cg.registerNames[reg] = null;
		isreg = false;
	}

}
