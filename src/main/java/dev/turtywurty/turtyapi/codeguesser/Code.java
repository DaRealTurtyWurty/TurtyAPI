package dev.turtywurty.turtyapi.codeguesser;

import lombok.Getter;

import java.util.Optional;

public record Code(String code, Language language) {
    @Getter
    public enum Language {
        JAVA("java", "java"), PYTHON("python", "py"),
        JAVASCRIPT("javascript", "js"), TYPESCRIPT("typescript", "ts"),
        C("c", "c"), CPP("cpp", "cpp"), CSHARP("csharp", "cs"),
        RUBY("ruby", "rb"), GO("go", "go"), KOTLIN("kotlin", "kt"),
        RUST("rust", "rs"), SWIFT("swift", "swift"), PHP("php", "php"),
        SCALA("scala", "scala"), HASKELL("haskell", "hs"),
        LUA("lua", "lua"), DART("dart", "dart"), R("r", "r"),
        PERL("perl", "pl"), GROOVY("groovy", "groovy"),
        FORTRAN("fortran", "f"), COBOL("cobol", "cob"),
        BASIC("basic", "bas"), PASCAL("pascal", "pas"),
        LISP("lisp", "lisp"), PROLOG("prolog", "pro"),
        SQL("sql", "sql"), HTML("html", "html"), CSS("css", "css"),
        XML("xml", "xml"), JSON("json", "json"), YAML("yaml", "yaml"),
        TOML("toml", "toml"), UNKNOWN("unknown", "??????");

        private final String name;
        private final String extension;

        Language(String name, String extension) {
            this.name = name;
            this.extension = extension;
        }

        public static Optional<Language> fromExtension(String extension) {
            for (Language language : values()) {
                if (language.getExtension().equals(extension)) {
                    return Optional.of(language);
                }
            }

            return Optional.empty();
        }
    }
}
