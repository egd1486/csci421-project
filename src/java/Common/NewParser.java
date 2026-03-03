package Common;
import Catalog.*;
import static Common.TokenType.*;
import java.util.ArrayList;

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

            if (T.Type == RPAREN) break; // Break once reached end of attributes (right paren)
        }

        // Now that we have all the attributes and have readched the rparen, check for semicolon
        Validate(Input[++Index], SEMICOLON);
        // If we got here, great. Create the table.
        Schema S = Catalog.AddSchema(Name);

        // Loop through and call through AddAttribute for validation,
        for (Attribute A : Attributes) 
        S.AddAttribute(A.name, A.type, A.typeLength, A.primaryKey, A.notNull, A.unique, A.defaultVal);

        // Return start of next command, which is past semicolon.
        return ++Index;
    }

    private static int Select(int Index, Token[] Input) throws Exception {
        boolean All = false;
        ArrayList<String> Columns = new ArrayList<>();
        ArrayList<String> Tables = new ArrayList<>();
        Token T = Input[Index++];

        // If star, do a normal display.
        if (T.Type == STAR) All = true;
        // Otherwise, read column names until we hit what's SUPPOSED to be from.
        else if (T.Type == NAME_LITERAL) {
            Columns.add(T.Literal);

            while ((T = Input[Index]).Type == COMMA) {
                Validate(T=Input[++Index], NAME_LITERAL);
                Columns.add(T.Literal);
                Index++;
            }
        } 
        // Other token types are not valid here.
        else throw new Exception("Unexpected token " + T.Type.toString() + ", expected * or column name(s).");

        // Now we get the tables, validate from first.
        Validate(Input[Index++], FROM);

        // At least one table,
        T = Input[Index++];
        Validate(T, NAME_LITERAL);
        Tables.add(T.Literal);

        // Parse more if available
        while (Input[Index].Type == COMMA) {
            T = Input[++Index];
            Validate(T, NAME_LITERAL);
            Tables.add(T.Literal);
            Index++;
        }

        // If we got here, great. Check for semicolon and complete the select.
        Validate(Input[Index], SEMICOLON);

        if (All && Tables.size() == 1) {
            Schema S = Catalog.GetSchema(Tables.get(0));

            if (S == null) throw new Exception("Table " + Tables.get(0) + " does not exist.");
            
            S.DisplayTable();
        }

        return ++Index;
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
        for (Token T : Input) System.out.println(T.Type.toString());
        try {
            while (Index < Input.length)
            switch (Input[Index++].Type) {
                case CREATE ->  Index = Create(Index, Input);
                case SELECT ->  Index = Select(Index, Input);
                case INSERT ->  Index = Insert(Index, Input);
                case ALTER  ->  Index = Alter(Index, Input);
                default     ->  throw new Exception("Unexpected token " + Input[Index-1].Type.toString());
            }
        } catch (Exception e) {
            if (e instanceof ArrayIndexOutOfBoundsException)
            throw new Exception("Unexpected end of input");
            else throw e;
        }


        
    }
}
