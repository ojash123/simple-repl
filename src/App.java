import simple.*;
import java.io.StringReader;
import java.util.Scanner;
import java_cup.runtime.Symbol;

public class App {
    public static void main(String[] args) {
        System.out.println("Simple REPL v1.0");

        // 1. Initialize Persistent Environments
        TypeChecker typeChecker = new TypeChecker();
        Env runtimeEnv = new Env();
        BigStep interpreter = new BigStep();

        Scanner scanner = new Scanner(System.in);

        // 2. The Loop
        while (true) {
            System.out.print("> ");
            if (!scanner.hasNextLine()) break;

            String input = scanner.nextLine().trim();
            if (input.isEmpty()) continue;
            if (input.equals("exit") || input.equals("quit")) break;

            try {
                // A. Parse
                SimpleLexer lexer = new SimpleLexer(new StringReader(input));
                parser p = new parser(lexer);
                Symbol resultSym = p.parse(); 
                AstNode result = (AstNode) resultSym.value;

                // B. Type Check
                TypeExpr type = typeChecker.check(result);
                if (type != null) {
                    System.out.println("Type: " + type);
                }

                // C. Execute
                interpreter.evaluate(result, runtimeEnv);

            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
                // e.printStackTrace(); // Uncomment for debugging
            }
        }
    }
}