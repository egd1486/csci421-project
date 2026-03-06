package Common;
import Common.*;
import java.util.ArrayList;
import static Common.TokenType.*;


public class Tokenizer {
        public static Token[] tokenize(String Input) throws Exception {
        ArrayList<Token> Tokens = new ArrayList<Token>();
        int i = 0, len = Input.length();

        while (i < len) {
            char c = Input.charAt(i);
            // Skip unusual whitespace
            if (Character.isWhitespace(c)) {i++; continue;}

            // Handle string literals
            if (c == '"') {
                i++;
                int start = i;
                // Iterate up the string until the closing quote,
                while (i < len && (Input.charAt(i) != '"')) i++;

                if (i >= len) throw new Exception("Malformed string literal.");

                // Once string is closed, add the token
                String word = Input.substring(start, i);
                Tokens.add(new Token(STRING_LITERAL, word));
                i++; // landed on quote, move to next char
                continue;
            }

            // If numeric, parse for number literal
            else if (Character.isDigit(c)) {
                boolean decimal = false;
                int start = i;
                i++;
                while (i < len && ((Character.isDigit(c = Input.charAt(i))) || (c == '.'))) {
                    if (c == '.') 
                    // if two decimals are encountered that's bad.
                    if (decimal) throw new Exception("Malformed double literal.");
                    // otherwise just flip the flag that you got one.
                    else decimal = true;

                    i++;
                }
                String word = Input.substring(start, i).toUpperCase();
                Tokens.add(new Token(decimal ? DOUBLE_LITERAL : INT_LITERAL, word));
                continue;
            }

            // Otherwise handle keywords.
            else if (Character.isLetter(c)) {
                int start = i;
                i++;
                while (i < len && (Character.isLetterOrDigit(c = Input.charAt(i)))) i++;
                String word = Input.substring(start, i).toUpperCase();
                TokenType T;
                // Evaluate as enum, treat as name literal otherwise.
                try {T = TokenType.valueOf(word);} catch (Exception E) {T = NAME_LITERAL;}
                Tokens.add(new Token(T, word));
                continue;
            }

            // Otherwise handle symbols
            // Otherwise, treat it as a symbol:
            else switch (c) {
                case ';' -> Tokens.add(new Token(SEMICOLON, ";"));
                case '(' -> Tokens.add(new Token(LPAREN, "("));
                case ')' -> Tokens.add(new Token(RPAREN, ")"));
                case '.' -> Tokens.add(new Token(PERIOD, "."));
                case ',' -> Tokens.add(new Token(COMMA, ","));
                case '=' -> Tokens.add(new Token(EQUAL, "="));
                case '*' -> Tokens.add(new Token(STAR, "*"));

                // multichar symbols
                case '<', '>' -> {
                    boolean Equal = (++i < len && Input.charAt(i) == '=');
                    boolean NotEqual = (i < len && Input.charAt(i) == '>');
                    boolean Less = (c == '<');
                    String S = Input.substring(i-1, i + ((Equal||NotEqual) ? 1:0));
                    Token T;

                    if (!Equal && !NotEqual) i--; // decrement i back if the lookahead did NOT show an equals.

                    // Handle <>
                    if (Less && NotEqual) 
                    T = new Token(NOT_EQUAL, S);

                    // Handle <= or >=,
                    else if (Equal) 
                    T = new Token(Less ? LESS_EQUAL : GREATER_EQUAL, S) ;
                
                    // otherwise just add the normal ones.
                    else 
                    T = new Token(Less ? LESS : GREATER, S);

                    Tokens.add(T);
                }

                default -> throw new Exception("Unknown symbol "+ c);
            }
            i++;
        }

        return Tokens.toArray(new Token[0]);
    }

}
