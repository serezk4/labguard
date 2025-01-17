package com.serezk4.core.lab.storage.adapter;

import com.google.gson.*;
import com.serezk4.core.antlr4.JavaLexer;
import com.serezk4.core.antlr4.JavaParser;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.lang.reflect.Type;

public class ParseTreeAdapter implements JsonSerializer<ParseTree>, JsonDeserializer<ParseTree> {

    @Override
    public JsonElement serialize(ParseTree src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(src.toStringTree());
    }

    @Override
    public ParseTree deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
        String parseTreeString = json.getAsString();

        CharStream charStream = CharStreams.fromString(parseTreeString);
        JavaLexer lexer = new JavaLexer(charStream);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        JavaParser parser = new JavaParser(tokens);
        parser.removeErrorListeners();

        return parser.compilationUnit();
    }
}
