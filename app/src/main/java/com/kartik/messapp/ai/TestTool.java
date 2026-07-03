package com.kartik.messapp.ai;

import com.google.ai.client.generativeai.type.FunctionDeclaration;
import com.google.ai.client.generativeai.type.FunctionType;
import com.google.ai.client.generativeai.type.Schema;
import com.google.ai.client.generativeai.type.Tool;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class TestTool {
    public static void test() {
        System.out.println("---- Schema Constructors ----");
        for (Constructor<?> c : Schema.class.getConstructors()) {
            System.out.println(c);
        }
        System.out.println("---- Tool Constructors ----");
        for (Constructor<?> c : Tool.class.getConstructors()) {
            System.out.println(c);
        }
        System.out.println("---- FunctionType Fields/Methods ----");
        for (java.lang.reflect.Field f : FunctionType.class.getDeclaredFields()) {
            System.out.println(f);
        }
        System.out.println("---- FunctionDeclaration Constructors ----");
        for (Constructor<?> c : FunctionDeclaration.class.getConstructors()) {
            System.out.println(c);
        }
    }
}
