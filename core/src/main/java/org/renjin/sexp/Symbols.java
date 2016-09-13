package org.renjin.sexp;

public class Symbols {



  private Symbols() {}

  public static final Symbol NAMES = Symbol.get("names");
  public static final Symbol DIM = Symbol.get("dim");
  public static final Symbol CLASS = Symbol.get("class");
  public static final Symbol LEVELS = Symbol.get("levels");
  public static final Symbol STDOUT = Symbol.get("stdout");
  public static final Symbol SRC_REF = Symbol.get("srcref");
  public static final Symbol ELLIPSES = Symbol.get("...");
  public static final Symbol SRC_FILE = Symbol.get("srcfile");
  public static final Symbol TEMP_VAL = Symbol.get("*tmp*");
  public static final Symbol DIMNAMES = Symbol.get("dimnames");
  public static final Symbol NAME = Symbol.get("name");
  public static final Symbol FQNAME = Symbol.get("fqname");
  public static final Symbol DOT_ENVIRONMENT = Symbol.get(".Environment");
  public static final Symbol COMMENT = Symbol.get("comment");
  public static final Symbol LEFT_BRACE = Symbol.get("{");

  public static final Symbol TZONE = Symbol.get("tzone");
  
  /**
   * Identifies the {@code row.names} attribute, which contains an {@code AtomicVector} with the
   * names of the rows of a {@code data.frame} object. Note: This attribute is different than the 
   * names of matrix rows: those are stored as an element in the {@code dimnames} attribute.
   */
  public static final Symbol ROW_NAMES = Symbol.get("row.names");
  public static final Symbol AS_CHARACTER = Symbol.get("as.character");
  public static final Symbol TEMP = Symbol.get("*tmp*");
  
  // S4 Symbols
  public static final Symbol PACKAGE = Symbol.get("package");
  public static final Symbol CLASS_NAME = Symbol.get("className");
  public static final Symbol PROTOTYPE = Symbol.get("prototype");
  public static final Symbol S4_BIT = Symbol.get("__S4_BIT");
  public static final Symbol S4_NULL = Symbol.get("\001NULL\001");
  
  public static final Symbol GENERIC = Symbol.get("generic");
  public static final Symbol SOURCE = Symbol.get("source");

  public static final Symbol DOT_DATA = Symbol.get(".Data");



  /**
   * A syntactically valid name consists of letters, numbers and the dot or underline characters and starts with
   * a letter or the dot not followed by a number. Names such as ".2way" are not valid,
   * and neither are the reserved words.
   *
   * @param name
   * @return true if {@code name} is syntatically valid
   */
  public static boolean isValid(String name) {
    if(!legalFirstCharacter(name)) {
      return false;
    }
    for(int i=1;i!=name.length();++i) {
      char c = name.charAt(i);
      if(!(Character.isLetter(c) || Character.isDigit(c) || c == '.' || c == '_')) {
        return false;
      }
    }
    return true;
  }

  public static boolean legalFirstCharacter(String name) {
    char first = name.charAt(0);
    return Character.isLetter(first) ||
        (first == '.' && !secondCharacterIsDigit(name));
  }

  private static boolean secondCharacterIsDigit(String name) {
    return name.length() >= 2 && Character.isDigit(name.codePointAt(1));
  }
}
