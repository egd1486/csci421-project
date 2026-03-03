package Common;
import Common.Type;
import Common.Attribute;
import java.util.ArrayList;
import static Common.TokenType.*;

public class NewParser {

    private static int Create(int Index, Token[] Input) throws Exception {
        // Validate syntax for "TABLE <name>"
        Token T; 
        Validate(Input[Index], TABLE);

        // Grab and validate name,
        Validate(T = Input[++Index], NAME_LITERAL);
        String Name = T.Literal;

        // Validate syntax for parenthesis
        Validate(Input[++Index], LPAREN);

        // Begin parsing attributes and their properties.
        ArrayList<Attribute> Attributes = new ArrayList<>();

        while (true) {
            // Get name
            Validate(T = Input[++Index], NAME_LITERAL);
            String AttributeName = T.Literal;

            // Get type
            T = Input[++Index];
            Type AType = null;
            int Length = 0;
            switch (T.Type) {
                case BOOLEAN -> AType = Type.BOOLEAN;
                case DOUBLE -> AType = Type.DOUBLE;
                case INTEGER -> AType = Type.INT;
                // Char types have a size provided, so must read that.
                case CHAR, VARCHAR -> {
                    AType = T.Type == CHAR ? Type.CHAR : Type.VARCHAR;

                    Validate(Input[++Index], LPAREN); // Paren 1
                    Validate(T = Input[++Index], INT_LITERAL); // Integer size,
                    Length = Integer.parseInt(T.Literal);
                    Validate(Input[++Index], RPAREN); // Paren 2
                }

                default -> throw new Exception("Unexpected token " + T.Type.toString() + ", expected column's type.");
            }

            // Create attribute object,
            Attribute A = new Attribute(AttributeName, AType, Length, false, false, false, null);

            // Now read tokens for qualifiers until comma or parenthesis is closed.
            while ((T = Input[++Index]).Type != COMMA && T.Type != RPAREN)
            switch (T.Type) {
                case PRIMARYKEY -> A.primaryKey = true;
                case NOTNULL -> A.notNull = true;
                case UNIQUE -> A.unique = true;
                case DEFAULT -> {
                    // Grab the provided default value..?
                    T = Input[++Index];
                    if (T.Type!=STRING_LITERAL && T.Type!=INT_LITERAL && T.Type!=DOUBLE_LITERAL && T.Type!=TRUE && T.Type!=FALSE) 
                    throw new Exception("Unexpected token " + T.Type.toString() + ", expected default value.");
                    A.defaultVal = A.Parse(T.Literal);
                }
                default -> throw new Exception("Unexpected token " + T.Type.toString() + ", expected attribute qualifier.");
            }    

            Attributes.add(A);

            if (T.Type == RPAREN) break;
        }

        

        return Index;
    }

    private static int Select(int Index, Token[] Input) throws Exception {


        return Index;
    }

    private static int Insert(int Index, Token[] Input) throws Exception {


        return Index;
    }

    private static int Alter(int Index, Token[] Input) throws Exception {


        return Index;
    }

    private static void Validate (Token Given, TokenType Expected) throws Exception {
        if (Given.Type != Expected)
        throw new Exception("Unexpected token " + Given.Type.toString() + ", expected " + Expected.toString());
    }

    public static void parse(Token[] Input) throws Exception {
        int Index = 0;
        while (Index < Input.length)
        switch (Input[Index++].Type) {
            case CREATE ->  Index = Create(Index, Input);
            case SELECT ->  Index = Select(Index, Input);
            case INSERT ->  Index = Insert(Index, Input);
            case ALTER  ->  Index = Alter(Index, Input);
            default     ->  throw new Exception("Unexpected token " + Input[Index].toString());
        }

        // for (Token T : Input) System.out.println(T.Type.toString());
    }
}
