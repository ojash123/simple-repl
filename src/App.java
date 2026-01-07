import simple.*;
import java.io.StringReader;
import java.util.Scanner;
import java_cup.runtime.Symbol;

public class App {
    public static void main(String[] args) {
        System.out.println("Simple REPL v2.0 (Multi-line & Typed)");
        System.out.println("Type 'exit' to quit.");

        // 1. Persistent State
        TypeChecker typeChecker = new TypeChecker(); // Persistent TypeEnv
        Interpreter interpreter = new Interpreter(); // New Interpreter
        Env runtimeEnv = new Env();                  // Persistent Runtime Env

        Scanner scanner = new Scanner(System.in);
        StringBuilder inputBuffer = new StringBuilder();

        while (true) {
            // Prompt changes based on whether we are inside a block
            if (inputBuffer.length() == 0) System.out.print("> ");
            else System.out.print("| ");

            if (!scanner.hasNextLine()) break;
            String line = scanner.nextLine();

            // Handle commands
            if (line.trim().equals("exit")) break;
            if (line.trim().isEmpty() && inputBuffer.length() == 0) continue;

            inputBuffer.append(line).append("\n");

            // 2. Multi-line Detection (Brace Counting)
            if (!isInputComplete(inputBuffer.toString())) {
                continue; // Keep reading lines
            }

            String input = inputBuffer.toString().trim();
            inputBuffer.setLength(0); // Reset buffer

            try {
                // 3. Parse
                SimpleLexer lexer = new SimpleLexer(new StringReader(input));
                parser p = new parser(lexer);
                // Ensure SimpleParser.cup has 'repl_unit' as the start symbol!
                Symbol resultSym = p.parse(); 
                AstNode result = (AstNode) resultSym.value;

                // 4. Type Check
                // This ensures types are valid and 'var's are recorded in TypeEnv
                TypeExpr type = typeChecker.check(result); 
                
                // If we get here, Type Checking Passed!
                if (type != null) {
                    System.out.println("Type: " + type.toString());
                }

                // 5. Execute
                interpreter.evaluate(result, runtimeEnv);

            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
                // e.printStackTrace(); // Useful for debugging crashes
                
                // If parser failed, the buffer is already cleared, so user types again.
            }
        }
    }

    // Heuristic: Input is complete if braces are balanced
    private static boolean isInputComplete(String code) {
        int braceCount = 0;
        for (char c : code.toCharArray()) {
            if (c == '{') braceCount++;
            else if (c == '}') braceCount--;
        }
        return braceCount <= 0;
    }
}