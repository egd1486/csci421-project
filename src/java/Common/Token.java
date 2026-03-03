package Common;

public class Token {
    public final TokenType Type;
    public final String Literal;

    public Token(TokenType Type, String Literal) {
        this.Type = Type;
        this.Literal = Literal;
    }
}
