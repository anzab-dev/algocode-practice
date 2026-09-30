/** IntelliJ live templates, offered as snippets: type the abbreviation and press Tab or Enter. */
export interface LiveTemplate {
  abbreviation: string;
  description: string;
  body: string;
  /** Only offered inside method bodies (false for class-level templates such as psvm). */
  statement: boolean;
}

export const LIVE_TEMPLATES: LiveTemplate[] = [
  { abbreviation: "sout", description: "Prints a string to System.out", body: "System.out.println($0);", statement: true },
  { abbreviation: "soutv", description: "Prints a value to System.out", body: 'System.out.println("${1:value} = " + ${1:value});$0', statement: true },
  { abbreviation: "souf", description: "Prints a formatted string", body: 'System.out.printf("${1}%n"${2});$0', statement: true },
  { abbreviation: "serr", description: "Prints a string to System.err", body: "System.err.println($0);", statement: true },
  { abbreviation: "psvm", description: "main() method declaration", body: "public static void main(String[] args) {\n\t$0\n}", statement: false },
  { abbreviation: "main", description: "main() method declaration", body: "public static void main(String[] args) {\n\t$0\n}", statement: false },
  { abbreviation: "fori", description: "Create iteration loop", body: "for (int ${1:i} = 0; ${1:i} < ${2:n}; ${1:i}++) {\n\t$0\n}", statement: true },
  { abbreviation: "forr", description: "Iterate in reverse", body: "for (int ${1:i} = ${2:n} - 1; ${1:i} >= 0; ${1:i}--) {\n\t$0\n}", statement: true },
  { abbreviation: "iter", description: "Iterate over an Iterable or array", body: "for (${1:var} ${2:item} : ${3:items}) {\n\t$0\n}", statement: true },
  { abbreviation: "ifn", description: "Inserts 'if null' statement", body: "if (${1:value} == null) {\n\t$0\n}", statement: true },
  { abbreviation: "inn", description: "Inserts 'if not null' statement", body: "if (${1:value} != null) {\n\t$0\n}", statement: true },
  { abbreviation: "whilet", description: "while (true) loop", body: "while (true) {\n\t$0\n}", statement: true },
  { abbreviation: "thr", description: "throw new", body: "throw new ${1:IllegalStateException}($0);", statement: true },
  { abbreviation: "psf", description: "public static final", body: "public static final ", statement: false },
  { abbreviation: "prsf", description: "private static final", body: "private static final ", statement: false },
];
