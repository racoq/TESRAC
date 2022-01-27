package ca.ubc.salt.model.composer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.PrimitiveType.Code;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import ca.ubc.salt.model.instrumenter.ClassModel;
import ca.ubc.salt.model.instrumenter.Method;
import ca.ubc.salt.model.instrumenter.ProductionClassInstrumenter;
import ca.ubc.salt.model.merger.BackwardTestMerger;
import ca.ubc.salt.model.state.StatementReadVariableVisitor;
import ca.ubc.salt.model.state.TestStatement;
import ca.ubc.salt.model.state.VarDefinitionPreq;
import ca.ubc.salt.model.utils.FileUtils;
import ca.ubc.salt.model.utils.Pair;
import ca.ubc.salt.model.utils.Settings;
import ca.ubc.salt.model.utils.Utils;

public class TestCaseComposer {

	public static Map<String, Set<String>> addedMethods = new HashMap<String, Set<String>>();
	public static LoadingCache<String, Map<String, String>> nameValuePairs;
	static {
		nameValuePairs = CacheBuilder.newBuilder().maximumSize(1000)
				.build(new CacheLoader<String, Map<String, String>>() { // build
																		// the
																		// cacheloader

					@Override
					public Map<String, String> load(String stmt) throws Exception {
						// make the expensive call
						return FileUtils.getNameValuePairs(stmt);
					}
				});
	}

	public static void updateRunningState(TestStatement statement, RunningState valueNamePairForCurrentState,
			Map<String, Map<String, String>> readVals, Map<String, Set<VarDefinitionPreq>> definitionPreq,
			Map<String, String> batchRename) {

		Map<String, String> renameMap = new HashMap<String, String>();

		findPreqVarsRenames(statement, valueNamePairForCurrentState, renameMap, readVals, definitionPreq, batchRename,
				null);

		findPostreqVarsRenames(statement, valueNamePairForCurrentState, renameMap);

		valueNamePairForCurrentState.update(statement, renameMap, definitionPreq);
		batchRename.putAll(renameMap);
	}

	public static Map<String, SimpleName> getAllVars(Statement statement) {
		if (statement == null)
			return null;
		StatementVariableVisitor srvv = new StatementVariableVisitor();
		statement.accept(srvv);
		return srvv.getVars();

	}

	public static Map<String, SimpleName> getAllRightHandSideVars(Statement statement) {
		if (statement == null)
			return null;
		StatementVariableVisitor srvv = new StatementVariableVisitor();
		if (statement.getNodeType() == ASTNode.EXPRESSION_STATEMENT) {
			ExpressionStatement exp = (ExpressionStatement) statement;
			org.eclipse.jdt.core.dom.Expression e = exp.getExpression();
			if (e instanceof Assignment) {
				Assignment a = (Assignment) e;
				a.getRightHandSide().accept(srvv);
			} else
				statement.accept(srvv);
		} else
			statement.accept(srvv);
		return srvv.getVars();

	}

	public static List<SimpleName> getSimpleNamesInTheStatement(ASTNode statement, Collection<SimpleName> vars) {
		if (statement == null)
			return null;
		StatementSimpleNameVisitor srvv = new StatementSimpleNameVisitor();
		statement.accept(srvv);
		Set<String> varNames = Utils.getNameSet(vars);
		List<SimpleName> refinedList = new LinkedList<SimpleName>();
		for (SimpleName sn : srvv.getVars()) {
			if (varNames.contains(sn.getIdentifier())) {
				ASTNode parent = sn.getParent();
				if (parent.getNodeType() == ASTNode.METHOD_INVOCATION) {
					MethodInvocation mi = (MethodInvocation) parent;
					if (mi.getName().equals(sn))
						continue;
				}
				refinedList.add(sn);
			}
		}

		return refinedList;

	}

	public static ASTNode rename(ASTNode stmt, Map<String, SimpleName> vars, Map<String, String> renameSet,
			Map<String, Pair<String, String>> castToMap) {

		ASTNode cpyStmt = ASTNode.copySubtree(stmt.getAST(), stmt);
		List<SimpleName> cpyVars = getSimpleNamesInTheStatement(cpyStmt, vars.values());

		for (SimpleName var : cpyVars) {
			boolean leftHandSide = false;
			String renamedVar = renameSet.get(var.getIdentifier());
			
			if (renamedVar == null && castToMap != null && castToMap.containsKey(var.getIdentifier())){
				renamedVar = var.getIdentifier();
			}
			
//			if(renamedVar == null){
//				System.out.println(var.getIdentifier());
//				System.exit(1);
//			}
			
			if (renamedVar != null) {
				ASTNode parentNode = var.getParent();

				if (parentNode.getNodeType() == ASTNode.VARIABLE_DECLARATION_FRAGMENT
						&& ((VariableDeclarationFragment) parentNode).getName().equals(var)) {
					
					VariableDeclarationFragment vdf = (VariableDeclarationFragment) parentNode;
					
					try{
						vdf.setName(vdf.getAST().newSimpleName(renamedVar));
					}
					catch(IllegalArgumentException e){
//						System.out.println(renamedVar);
//						System.exit(1);
//						if(renamedVar.length() == 0){
//							renamedVar = Integer.toString(renamedVar.hashCode());
//						} else if (renamedVar.contains(".")){
//							renamedVar = renamedVar.replaceAll(".", "");
//						}
//						
//						vdf.setName(vdf.getAST().newSimpleName(renamedVar));
					}

				} else {
					if (isLeftHandSide(parentNode, var)) {
						if (castToMap != null && castToMap.containsKey(var.getIdentifier())) {
							Assignment a = (Assignment) parentNode;
							AST ast = a.getAST();
							ParenthesizedExpression pe = ast.newParenthesizedExpression();
							// Expression reCpy = (Expression)
							// ASTNode.copySubtree(ast, a.getRightHandSide());
							Expression reCpy = a.getRightHandSide();
							a.setRightHandSide(ast.newParenthesizedExpression());
							pe.setExpression((Expression) getCastStructure(
									castToMap.get(var.getIdentifier()).getFirst(), var.getIdentifier(), ast, reCpy));
							a.setRightHandSide(pe);
						}
						leftHandSide = true;
					}

					int index = renamedVar.lastIndexOf('.');
					if (index == -1) {
						Pair<String, String> pair = castToMap.get(var.getIdentifier());
						if (leftHandSide == false && castToMap != null && pair != null && pair.getSecond() != null
								&& !pair.getSecond().equals("")) {
							AST ast = var.getAST();
							SimpleName varCpy = ast.newSimpleName(renamedVar);
							ASTNode replacement = getCastStructure(castToMap.get(var.getIdentifier()).getSecond(),
									var.getIdentifier(), ast, varCpy);
							try {
								replaceSimpleNameWithASTNode(var, parentNode, replacement);
							} catch (Exception e) {
								Settings.consoleLogger.error("something wrong with " + parentNode.toString() + " , "
										+ replacement.toString());
								replaceSimpleNameWithASTNode(var, parentNode, ast.newSimpleName(renamedVar));
							}
						} else
							var.setIdentifier(renamedVar);

					} else {
						String q = renamedVar.substring(0, index);
						String v = renamedVar.substring(index + 1);
						AST ast = parentNode.getAST();

						ASTNode replacement = ast.newQualifiedName(ast.newName(q), ast.newSimpleName(v));
						Pair<String, String> pair = castToMap.get(var.getIdentifier());
						if (leftHandSide == false && castToMap != null && pair != null && pair.getSecond() != null
								&& !pair.getSecond().equals("")) {
							replacement = getCastStructure(castToMap.get(var.getIdentifier()).getSecond(),
									var.getIdentifier(), ast, replacement);
						}

						replaceSimpleNameWithASTNode(var, parentNode, replacement);
					}
				}
			}
		}

		if (cpyStmt instanceof VariableDeclarationStatement) {
			VariableDeclarationStatement vds = (VariableDeclarationStatement) cpyStmt;
			vds.modifiers().clear();
		}
		return cpyStmt;
	}

	private static boolean isLeftHandSide(ASTNode parentNode, SimpleName sn) {
		if (parentNode.getNodeType() == ASTNode.ASSIGNMENT) {
			Assignment a = (Assignment) parentNode;
			if (a.getLeftHandSide().getNodeType() == ASTNode.SIMPLE_NAME) {
				SimpleName left = (SimpleName) a.getLeftHandSide();
				if (left.equals(sn))
					return true;
			}
		}
		return false;
	}

	private static void replaceSimpleNameWithASTNode(SimpleName simpleName, ASTNode parent, ASTNode replacement) {
		StructuralPropertyDescriptor property = simpleName.getLocationInParent();
		if (property instanceof ChildListPropertyDescriptor) {
			List params = null;
			if (parent instanceof MethodInvocation)
				params = ((MethodInvocation) parent).arguments();
			if (parent instanceof ClassInstanceCreation)
				params = ((ClassInstanceCreation) parent).arguments();
			if (parent instanceof InfixExpression)
				params = ((InfixExpression) parent).extendedOperands();
			if (params != null) {
				int counter = 0;
				for (Iterator it = params.listIterator(); it.hasNext(); counter++) {
					Object ex = it.next();
					if (ex instanceof SimpleName) {
						if (ex.equals(simpleName)) {
							it.remove();
							params.add(counter, replacement);
							break;
						}

					}
				}
			} else {
				Settings.consoleLogger.error("unsupported type renaming");
				BackwardTestMerger.mergingResult.warning = true;
			}
		} else {
			parent.setStructuralProperty(simpleName.getLocationInParent(), replacement);
		}
	}

	private static Type getType(String type, AST ast) {
		Block a = (Block) Utils.createBlockWithText(String.format("Object b = (%s)a;", type));
		VariableDeclarationStatement vds = (VariableDeclarationStatement) a.statements().get(0);
		Type tp = ((CastExpression) ((VariableDeclarationFragment) vds.fragments().get(0)).getInitializer()).getType();
		if (tp instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType) tp;
			pt.typeArguments().clear();
			tp = pt.getType();

		}
		Type tpcpy = (Type) ASTNode.copySubtree(ast, tp);
		return tpcpy;
		// int counter = 0;
		// for (int i = 0; i < type.length();i++)
		// {
		// if (type.charAt(i) == '[')
		// counter++;
		// }
		// if (counter == 0)
		// return ast.newSimpleType(ast.newName(type));
		//
		// int index = type.indexOf('[');
		// type = type.substring(0, index);
		//
		// Type tp = null;
		// Code code = PrimitiveType.toCode(type);
		// if (code != null)
		// tp = ast.newPrimitiveType(code);
		// else
		// tp = ast.newSimpleType(ast.newName(type));
		//
		//
		// ArrayType arrType = ast.newArrayType(tp, counter);
		// return arrType;

	}

	private static ASTNode getCastStructure(String type, String varName, AST ast, ASTNode replacement) {
		// String type = castToMap.get(varName).getSecond();
		CastExpression ce = ast.newCastExpression();
		ce.setExpression((Expression) replacement);

		ce.setType(getType(type, ast));
		ParenthesizedExpression pe = ast.newParenthesizedExpression();
		pe.setExpression(ce);
		return pe;
	}

	public static String generateTestCaseName(Set<String> testCases) {
		StringBuilder sb = new StringBuilder();

		for (String testCase : testCases) {
			sb.append(Utils.getTestCaseName(testCase));
			sb.append('_');
		}
		sb.setLength(sb.length() - 1);
		return sb.toString();
	}

	// public static void composeTestCases(List<Pair<Set<String>,
	// List<List<TestStatement>>>> mergedTestCases)
	// {
	//
	// Utils.copyProject(Settings.PROJECT_PATH, Settings.PROJECT_MERGED_PATH);
	//
	// for (Pair<Set<String>, List<List<TestStatement>>> pair : mergedTestCases)
	// {
	// Set<String> connectedComponent = pair.getFirst();
	// List<List<TestStatement>> testCases = pair.getSecond();
	// String name = generateTestCaseName(connectedComponent);
	// if (testCases.size() == 1)
	// composeTestCase(testCases.get(0), connectedComponent, name);
	// else
	// for (int i = 0; i < testCases.size(); i++)
	// {
	// composeTestCase(testCases.get(i), connectedComponent, name + i);
	// }
	// }
	// }

	public static void composeTestCase(List<TestStatement> path, Set<String> testCases, String name,
			Map<String, Map<String, String>> readVals, Map<String, Set<VarDefinitionPreq>> definitionPreq,
			List<TestStatement> additionalStmts) {

		// populateStateField(path);

		Map<String, Set<String>> testClasses = Utils.getTestClassMapFromTestCases(testCases);

		String mainClassName = Utils.getTestClassWithMaxNumberOfTestCases(testClasses);

		List<TestStatement> renamedStatements = performRenaming(path, testCases, mainClassName, readVals,
				definitionPreq);
		renamedStatements.addAll(additionalStmts);

		try {
			ComposerHelper.writeBackMergedTestCases(renamedStatements, testCases, name, testClasses, mainClassName);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// StringBuilder sb = new StringBuilder();
		// for (TestStatement statement : path)
		// {
		// sb.append(statement.getName() + " : "
		// + (statement.statement == null ? "null" :
		// statement.statement.toString()));
		// sb.append('\n');
		// }
		// System.out.println(sb.toString());

	}

	private static List<FieldDeclaration> getRequiredFieldDecs(List<TestStatement> path, String mainClassName) {

		List<FieldDeclaration> fieldDecStatements = new ArrayList<FieldDeclaration>();

		Map<String, Set<String>> fieldVars = new HashMap<String, Set<String>>();
		for (TestStatement statement : path) {
			Statement stmt = statement.statement;
			StatementFieldVisitor sfv = new StatementFieldVisitor(fieldVars);
			stmt.accept(sfv);
			// renameFieldVarsInStmt(statement, sfv);
		}

		for (Entry<String, Set<String>> entry : fieldVars.entrySet()) {
			String className = entry.getKey();

			if (className.equals(mainClassName))
				continue;

			Set<String> usedFields = entry.getValue();

			String testClassPath = Utils.getClassFile(className);

			if (testClassPath == null)
				continue;

			String source;
			try {
				source = FileUtils.readFileToString(testClassPath);
				Document document = new Document(source);
				List<ClassModel> classes = ClassModel.getClasses(document.get(), true, testClassPath,
						new String[] { Settings.PROJECT_PATH}, new String[] { Settings.LIBRARY_JAVA });
				for (ClassModel clazz : classes) {
					List<FieldDeclaration> classFields = clazz.getAllFields();
					for (FieldDeclaration fieldDec : classFields) {
						for (Object varDecObj : fieldDec.fragments()) {
							if (varDecObj instanceof VariableDeclarationFragment) {
								VariableDeclarationFragment varDec = (VariableDeclarationFragment) varDecObj;
								SimpleName varInFieldDec = varDec.getName();
								if (usedFields.contains(varInFieldDec.getIdentifier())) {
									Map<String, SimpleName> varSet = new HashMap<String, SimpleName>();
									varSet.put(varInFieldDec.getIdentifier(), varInFieldDec);
									Map<String, String> renameMap = new HashMap<String, String>();
									renameMap.put(varInFieldDec.getIdentifier(),
											varInFieldDec.getIdentifier() + "_" + className);
									fieldDecStatements
											.add((FieldDeclaration) rename(fieldDec, varSet, renameMap, null));
								}

							}
						}
					}
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		return fieldDecStatements;
	}

	private static void renameFieldVarsInStmt(TestStatement statement, StatementFieldVisitor sfv) {
		HashMap<String, String> renameMap = new HashMap<String, String>();
		for (SimpleName sn : sfv.fields.values()) {
			renameMap.put(sn.getIdentifier(), sn.getIdentifier() + "_" + sfv.className);
		}

		Statement renamedStmt = (Statement) rename(statement.refactoredStatement, sfv.fields, renameMap, null);
		statement.refactoredStatement = renamedStmt;
	}

	private static void addFieldDecsToPath(List<FieldDeclaration> fieldDecs, List<ASTNode> path) {
		for (FieldDeclaration fieldDec : fieldDecs) {
			List mds = fieldDec.modifiers();
			for (Iterator iterator = mds.iterator(); iterator.hasNext();) {
				Object obj = iterator.next();
				if (obj instanceof Modifier) {
					Modifier mod = (Modifier) obj;
					if (!mod.isFinal())
						iterator.remove();

				}
			}
			path.add(0, fieldDec);
		}
	}

	public static void getNonTestMethods(ClassModel clazz, Set<Method> testMethods) {
		List<Method> methods = clazz.getMethods();
		for (Method method : methods) {
			if (!method.isTestMethod())
				testMethods.add(method);
		}
	}

	public static List<MethodInvocation> getTestMethodInvocations(TestStatement stmt) {

		Set<String> classes = new HashSet<String>();
		classes.add(Utils.getTestCaseName(Utils.getTestClassNameFromTestStatement(stmt.getName())));
		List<String> parents = Utils.getAllParents(Utils.getTestClassNameFromTestStatement(stmt.getName()));
		for (String parent : parents)
			classes.add(parent);
		TestMethodInvocationVisitor smiv = new TestMethodInvocationVisitor(classes);
		stmt.statement.accept(smiv);
		return smiv.getMethodInvocations();
	}

	public static List<MethodInvocation> getMethodInvocations(Statement stmt) {
		MethodInvocationVisitor smiv = new MethodInvocationVisitor();
		stmt.accept(smiv);
		return smiv.getMethodInvocations();
	}

	public static Statement renameMethodInvocs(TestStatement stmt) {
		Statement cpyStmt = (Statement) ASTNode.copySubtree(stmt.refactoredStatement.getAST(),
				stmt.refactoredStatement);
		List<MethodInvocation> methodInvocs = getMethodInvocations(cpyStmt);
		List<MethodInvocation> testMethodInvocation = getTestMethodInvocations(stmt);
		Set<String> testMethodNames = Utils.getNames(testMethodInvocation);
		for (MethodInvocation methodCall : methodInvocs) {
			if (testMethodNames.contains(methodCall.getName().getIdentifier())) {
				String renamedVar = methodCall.getName().getIdentifier() + "_"
						+ Utils.getTestClassNameFromTestStatement(stmt.getName());
				methodCall.setName(methodCall.getAST().newSimpleName(renamedVar));
			}
		}
		return cpyStmt;
	}

	public static void renameMethodCalls(List<TestStatement> originalStatements, String mainClass) {
		for (TestStatement statement : originalStatements) {
			if (statement.statement == null)
				continue;
			if (!mainClass.equals(Utils.getTestClassNameFromTestStatement(statement.getName())))
				statement.refactoredStatement = renameMethodInvocs(statement);
		}
	}

	public static List<ASTNode> getPathFromStatements(List<TestStatement> statements) {
		List<ASTNode> path = new ArrayList<ASTNode>();
		for (TestStatement stmt : statements) {
			path.add(stmt.refactoredStatement);
		}
		return path;
	}

	private static void writeBackMergedTestCases(List<TestStatement> originalStatements, Set<String> testCases,
			String name, Map<String, Set<String>> testClasses, String mainClassName) throws IOException {

		// class -> testCases
		renameMethodCalls(originalStatements, mainClassName);

		List<FieldDeclaration> fieldDecs = getRequiredFieldDecs(originalStatements, mainClassName);
		List<ASTNode> path = getPathFromStatements(originalStatements);
		addFieldDecsToPath(fieldDecs, path);

		Map<String, ImportDeclaration> imports = new HashMap<String, ImportDeclaration>();

		Set<Method> nonTestMethods = new HashSet<Method>();

		while (!testClasses.isEmpty()) {

			String testClassName = Utils.getTestClassWithMaxNumberOfTestCases(testClasses);

			Document document = getDocumentForClassName(testClassName);
			String testClassPath = Utils.getClassFileForProjectPath(testClassName, Settings.PROJECT_MERGED_PATH);
			List<ClassModel> classes = ClassModel.getClasses(document.get(), true, testClassPath,
					new String[] { Settings.PROJECT_PATH }, new String[] { Settings.LIBRARY_JAVA });

			Set<String> testCasesOfClass = testClasses.get(testClassName);

			ASTRewrite rewriter = ASTRewrite.create(classes.get(0).getCu().getAST());

			for (ClassModel clazz : classes) {
				if (clazz.getTypeDec().getName().toString().equals(testClassName)) {

					ListRewrite listRewrite = rewriter.getListRewrite(clazz.getTypeDec(),
							TypeDeclaration.BODY_DECLARATIONS_PROPERTY);

					removeTestCasesFromTestClass(clazz, testCasesOfClass, rewriter);

					if (testClassName.equals(mainClassName)) {
						Settings.consoleLogger
								.error(String.format("adding tests to %s", Utils.getClassFile(mainClassName)));
						addMergedTestCase(path, name, clazz, listRewrite);
						// System.out.println(getMergedMethod(path, name,
						// clazz.getTypeDec().getAST()).toString());
					} else {
						getNonTestMethods(clazz, nonTestMethods);
						// adding other imports
						getAllImports(imports, clazz);
					}

				}
			}

			TextEdit edits = rewriter.rewriteAST(document, null);
			try {
				edits.apply(document);
			} catch (MalformedTreeException | BadLocationException e) {
				e.printStackTrace();
			}

			Utils.addImports(document, Arrays.asList(new String[] { "org.junit.Ignore" }));

			Utils.writebackSourceCode(document,
					Utils.getClassFileForProjectPath(testClassName, Settings.PROJECT_MERGED_PATH));
			// System.out.println(document.get());

			testClasses.remove(testClassName);
			// parse each test class
			// remove and replace
			// if they have variables var with isField() add TestClass x = new
			// TestClass(); var -> x.var
		}

		addImportsAndNonTestMethodsToMainClass(nonTestMethods, mainClassName, imports);
	}

	static void getAllImports(Map<String, ImportDeclaration> imports, ClassModel clazz) {
		ITypeBinding bind = clazz.getTypeDec().resolveBinding();
		imports.put(bind.getQualifiedName(), null);
		for (Object obj : clazz.getCu().imports()) {
			if (obj instanceof ImportDeclaration) {
				ImportDeclaration imDec = (ImportDeclaration) obj;
				if (imDec.isStatic())
					imports.put("static " + imDec.getName().getFullyQualifiedName(), imDec);
				else
					imports.put(imDec.getName().getFullyQualifiedName(), imDec);
			}
		}
	}

	private static void addNonTestMethods(Set<Method> nonTestMethods, Document document, String mainClassName) {
		try {
			List<ClassModel> classes = ClassModel.getClasses(document.get());
			ASTRewrite rewriter = ASTRewrite.create(classes.get(0).getCu().getAST());
			for (ClassModel clazz : classes) {
				if (clazz.getTypeDec().getName().toString().equals(mainClassName)) {
					ListRewrite listRewrite = rewriter.getListRewrite(clazz.getTypeDec(),
							TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
					List bodyDecs = clazz.getTypeDec().bodyDeclarations();

					for (Method nonTestMethod : nonTestMethods) {
						String newName = nonTestMethod.getMethodDec().getName().toString() + "_"
								+ nonTestMethod.getClassName();
						String signature = newName + "(" + nonTestMethod.getMethodDec().parameters().toString() + ")";
						if (Utils.containsInSetInMap(addedMethods, mainClassName, signature))
							continue;
						AST ast = clazz.getTypeDec().getAST();
						MethodDeclaration methodCpy = (MethodDeclaration) ASTNode.copySubtree(ast,
								nonTestMethod.getMethodDec());
						methodCpy.setName(ast.newSimpleName(newName));

						listRewrite.insertLast(methodCpy, null);
						Utils.addToTheSetInMap(addedMethods, mainClassName, signature);
					}
				}
			}
			TextEdit edits = rewriter.rewriteAST(document, null);
			edits.apply(document);

		} catch (IOException | MalformedTreeException | BadLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void addImportsAndNonTestMethodsToMainClass(Set<Method> nonTestMethods, String mainClassName,
			Map<String, ImportDeclaration> imports) throws IOException {
		Document document = getDocumentForClassName(mainClassName);
		if (nonTestMethods != null)
			addNonTestMethods(nonTestMethods, document, mainClassName);
		Utils.addImports(document, imports);
		Utils.writebackSourceCode(document,
				Utils.getClassFileForProjectPath(mainClassName, Settings.PROJECT_MERGED_PATH));
	}

	public static Document getDocumentForClassName(String testClassName) throws IOException {
		String testClassPath = Utils.getClassFileForProjectPath(testClassName, Settings.PROJECT_MERGED_PATH);

		File file = new File(testClassPath);
		if (!file.exists())
			Utils.copyProject(Settings.PROJECT_PATH, Settings.PROJECT_MERGED_PATH);

		String source = FileUtils.readFileToString(testClassPath);
		Document document = new Document(source);
		return document;
	}

	static void addMergedTestCase(List<ASTNode> path, String name, ClassModel clazz, ListRewrite listRewrite) {
		TypeDeclaration td = clazz.getTypeDec();
		AST ast = td.getAST();
		
		MethodDeclaration md = getMergedMethod(path, name, ast);

		// System.out.println(md.toString());
		// clazz.getTypeDec().bodyDeclarations().add(md);
		listRewrite.insertLast(md, null);
	}

	private static MethodDeclaration getMergedMethod(List<ASTNode> path, String name, AST ast) {
		MethodDeclaration md = ast.newMethodDeclaration();
		md.setName(ast.newSimpleName(name));

		MarkerAnnotation ma = ast.newMarkerAnnotation();
		ma.setTypeName(ast.newName("Test"));

		md.modifiers().add(ma);

		md.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD));

		ASTNode methodBlock = Utils.createBlockWithText(getTestMethodText(path));

		Block methodBlockWithAST = (Block) ASTNode.copySubtree(ast, methodBlock);

		md.setBody(methodBlockWithAST);
		return md;
	}

	public static String getTestMethodText(List<ASTNode> path) {
		StringBuilder sb = new StringBuilder();
		// sb.append(String.format("public void %s(){", name));
		for (ASTNode ts : path) {
			if (ts != null) {
				sb.append(ts.toString());
				sb.append('\n');
			}
		}
		// sb.append("}");

		return sb.toString();

	}

	public static String getTestMethodText(List<TestStatement> path, int n) {
		StringBuilder sb = new StringBuilder();
		// sb.append(String.format("public void %s(){", name));
		for (int j = 0; j <= n; j++) {
			TestStatement ts = path.get(j);
			if (ts.statement != null) {
				sb.append(ts.statement.toString());
				sb.append('\n');
			}
		}
		// sb.append("}");

		return sb.toString();

	}

	public static void removeTestCasesFromTestClass(ClassModel clazz, Set<String> testCasesOfClass,
			ASTRewrite rewrite) {
		// Settings.consoleLogger.error(String.format("removing %s from %s",
		// testCasesOfClass, clazz.getTypeDec().getName().toString()));
		List<Method> methods = clazz.getMethods();

		for (Method m : methods) {
			String methodName = m.getFullMethodName();
			if (testCasesOfClass.contains(methodName)) {
				ListRewrite listRewrite = rewrite.getListRewrite(m.getMethodDec(),
						MethodDeclaration.MODIFIERS2_PROPERTY);
				// clazz.getTypeDec().bodyDeclarations().remove(m.getMethodDec());
				AST ast = m.getMethodDec().getAST();
				MarkerAnnotation ma = ast.newMarkerAnnotation();
				ma.setTypeName(ast.newName("Ignore"));
				listRewrite.insertFirst(ma, null);
			}
		}

	}

	public static void removeTestCasesFromTestClass(ClassModel clazz, Set<String> testCasesOfClass) {
		// Settings.consoleLogger.error(String.format("removing %s from %s",
		// testCasesOfClass, clazz.getTypeDec().getName().toString()));
		List<Method> methods = clazz.getMethods();

		for (Method m : methods) {
			String methodName = m.getFullMethodName();
			if (testCasesOfClass.contains(methodName)) {
				AST ast = m.getMethodDec().getAST();
				MarkerAnnotation ma = ast.newMarkerAnnotation();
				ma.setTypeName(ast.newName("Ignore"));
				m.getMethodDec().modifiers().add(ma);
			}
		}

	}

	public static void reAddTestCasesFromTestClass(ClassModel clazz, Set<String> testCasesOfClass, ASTRewrite rewrite,
			boolean flag) {
		// Settings.consoleLogger.error(String.format("removing %s from %s",
		// testCasesOfClass, clazz.getTypeDec().getName().toString()));
		List<Method> methods = clazz.getMethods();

		for (Method m : methods) {
			String methodName = m.getFullMethodName();
			if (testCasesOfClass.contains(methodName)) {
				ListRewrite listRewrite = rewrite.getListRewrite(m.getMethodDec(),
						MethodDeclaration.MODIFIERS2_PROPERTY);
				List modifs = m.getMethodDec().modifiers();
				if (flag && m.getMethodDec().toString().contains("return;"))
					System.out.println(m.getFullMethodName());
				for (Iterator it = modifs.listIterator(); it.hasNext();) {
					Object obj = it.next();
					if (obj instanceof MarkerAnnotation) {
						MarkerAnnotation ma = (MarkerAnnotation) obj;
						if (ma.getTypeName().toString().contains("Ignore")) {
							listRewrite.remove(ma, null);
						}
					}
				}
			}
		}

	}

	public static Statement renameTestStatement(TestStatement statement, RunningState valueNamePairForCurrentState,
			Map<String, Map<String, String>> readVals, Map<String, Set<VarDefinitionPreq>> definitionPreq,
			Map<String, String> batchRename) {
		Map<String, String> renameMap;
		if (statement.renameMap == null)
			renameMap = new HashMap<String, String>();
		else
			renameMap = statement.renameMap;

		Map<String, Pair<String, String>> castToMap = new HashMap<String, Pair<String, String>>();

		findPreqVarsRenames(statement, valueNamePairForCurrentState, renameMap, readVals, definitionPreq, batchRename,
				castToMap);

		findPostreqVarsRenames(statement, valueNamePairForCurrentState, renameMap);

		Map<String, SimpleName> vars = statement.getAllVars();
		if (vars == null)
			return null;

		valueNamePairForCurrentState.update(statement, renameMap, definitionPreq);

		batchRename.putAll(renameMap);

		return (Statement) rename(statement.statement, vars, renameMap, castToMap);
	}

	private static List<TestStatement> performRenaming(List<TestStatement> path, Set<String> testCases,
			String mainClassName, Map<String, Map<String, String>> readVals,
			Map<String, Set<VarDefinitionPreq>> definitionPreq) {

		if (path.size() == 0)
			return path;
		RunningState runningState = new RunningState(testCases, mainClassName);

		List<TestStatement> renamedStatements = cloneStatements(path);

		Map<String, String> batchRename = new HashMap<String, String>();
		BackwardTestMerger.populateGoalsInStatements(definitionPreq, readVals, runningState, renamedStatements);
		for (TestStatement statement : renamedStatements) {
			Statement renamedStatement = renameTestStatement(statement, runningState, readVals, definitionPreq,
					batchRename);
			statement.refactoredStatement = renamedStatement;
		}

		return renamedStatements;
	}

	public static List<TestStatement> performRenamingWithRunningState(List<TestStatement> path, Set<String> testCases,
			String mainClassName, Map<String, Map<String, String>> readVals,
			Map<String, Set<VarDefinitionPreq>> definitionPreq, RunningState runningState) {

		List<TestStatement> renamedStatements = cloneStatements(path);

		Map<String, String> batchRename = new HashMap<String, String>();
		BackwardTestMerger.populateGoalsInStatements(definitionPreq, readVals, runningState, renamedStatements);
		for (TestStatement statement : renamedStatements) {
			Statement renamedStatement = renameTestStatement(statement, runningState, readVals, definitionPreq,
					batchRename);
			statement.refactoredStatement = renamedStatement;
		}

		return renamedStatements;
	}

	public static List<TestStatement> cloneStatements(List<TestStatement> path) {
		List<TestStatement> renamedStatements = new ArrayList<TestStatement>();
		for (TestStatement statement : path) {

			if (statement.statement == null)
				continue;

			TestStatement cpyStatement = null;
			try {
				cpyStatement = statement.clone();
			} catch (CloneNotSupportedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			renamedStatements.add(cpyStatement);
		}
		return renamedStatements;
	}

	private static void findPostreqVarsRenames(TestStatement testStatement, RunningState curState,
			Map<String, String> renameMap) {
		if (testStatement.statement == null)
			return;
		Statement stmt = testStatement.statement;
		StatementDefineVariableVisitor sdvv = new StatementDefineVariableVisitor();
		stmt.accept(sdvv);
		Set<SimpleName> vars = sdvv.getVars();
		for (SimpleName var : vars) {
			String varInStmt = var.getIdentifier();
			// if (renameMap.containsKey(varInStmt))
			// varInStmt = renameMap.get(varInStmt);
			String varInState = curState.getValue(varInStmt);
			if (varInState != null) {
				String newVarInStmt = varInStmt + "_"
						+ Utils.getTestCaseNameFromTestStatementWithoutClassName(testStatement.getName());

				String newName = newVarInStmt;
				int counter = 2;
				while (curState.getValue(newName) != null) {
					newName = newVarInStmt + counter;
					counter++;
				}
				renameMap.put(varInStmt, newName);
			}
		}
	}

	private static void findPreqVarsRenames(TestStatement stmt, RunningState runningState,
			Map<String, String> renameMap, Map<String, Map<String, String>> readVals,
			Map<String, Set<VarDefinitionPreq>> definitionPreq, Map<String, String> batchRename,
			Map<String, Pair<String, String>> castToMap) {
		// if
		// (stmt.statement.toString().contains("Assert.assertTrue(mColumn3[0]"))
		// System.out.println();
		Map<String, String> nameValuePairOfStmtBefore = readVals.get(stmt.getName());

		if (nameValuePairOfStmtBefore == null)
			return;

		Set<String> chosenNames = new HashSet<String>();
		for (Entry<String, String> entry : nameValuePairOfStmtBefore.entrySet()) {

			String varNameInStmt = entry.getKey();

			if (renameMap.containsKey(varNameInStmt))
				continue;
			String value = entry.getValue();

			Set<String> varNameInState = runningState.getName(value);
			if (varNameInState == null || varNameInState.size() == 0) {
				Settings.consoleLogger.error(
						String.format("something's wrong with %s--%s", stmt.getName(), stmt.statement.toString()));
				BackwardTestMerger.testCasesToRemove.add(Utils.getTestCaseNameFromTestStatement(stmt.getName()));
				BackwardTestMerger.mergingResult.fatalError = true;
				BackwardTestMerger.testCasesToRemove.add(Utils.getTestCaseNameFromTestStatement(stmt.getName()));
			} else if (!varNameInState.contains(varNameInStmt)) {
				// TODO for choosing the varname do a edit distance and choose
				// the most simillar one
				boolean success = false;
				for (String name : varNameInState) {
					if (!chosenNames.contains(name)) {
						renameMap.put(varNameInStmt, name);
						if (castToMap != null) {
							String typeInStmt = stmt.getTypeOfVar(varNameInStmt);
							String typeInState = runningState.getType(name);
							if (!typeInState.equals(typeInStmt)) {
								castToMap.put(varNameInStmt, new Pair<String, String>(typeInState, typeInStmt));
							}
						}
						chosenNames.add(name);
						success = true;
						break;
					}
				}
				if (success != true) {
					Settings.consoleLogger.error(
							String.format("something's wrong with %s--%s", stmt.getName(), stmt.statement.toString()));
					BackwardTestMerger.mergingResult.fatalError = true;
					BackwardTestMerger.testCasesToRemove.add(Utils.getTestCaseNameFromTestStatement(stmt.getName()));
				}
			} else {
				chosenNames.add(varNameInStmt);
				if (castToMap != null) {
					String typeInStmt = stmt.getTypeOfVar(varNameInStmt);
					String typeInState = runningState.getType(varNameInStmt);
					if (!typeInState.equals(typeInStmt)) {
						castToMap.put(varNameInStmt, new Pair<String, String>(typeInState, typeInStmt));
					}
				}
			}
		}

		Set<VarDefinitionPreq> defPreqs = definitionPreq.get(stmt.getName());
		if (defPreqs != null) {
			for (VarDefinitionPreq defPreq : defPreqs) {
				String neededType = defPreq.getType();
				Set<String> varsInState = runningState.getNameForType(neededType);
				if (varsInState == null || varsInState.isEmpty()) {
					Settings.consoleLogger.error(
							String.format("something's wrong with %s--%s", stmt.getName(), stmt.statement.toString()));
					BackwardTestMerger.mergingResult.fatalError = true;
					BackwardTestMerger.testCasesToRemove.add(Utils.getTestCaseNameFromTestStatement(stmt.getName()));
				} else {
					if (renameMap.containsKey(defPreq.getName().getIdentifier()))
						continue;
					if (stmt.readGoals != null) {
						boolean renamed = false;
						for (String varName : varsInState) {
							String value = runningState.getValue(varName);
							Set<String> neededVals = stmt.readGoals.get(value);
							Set<String> namesWithThisValue = runningState.getName(value);
							if (neededVals == null || neededVals.size() <= namesWithThisValue.size() - 1) {
								renameMap.put(defPreq.getName().getIdentifier(), varName);
								renamed = true;
								break;
							}
						}

						if (renamed == false) {
							Settings.consoleLogger.error(String.format("renaming happend for  %s--%s", stmt.getName(),
									stmt.statement.toString()));
						}
					} else {

						String renamedName = renameMap.get(defPreq.getName().getIdentifier());
						if (renamedName == null) {
							renamedName = defPreq.getName().getIdentifier();
						}

						if ((runningState.getValue(renamedName) == null
								|| !runningState.getType(renamedName).equals(neededType))
								|| !batchRename.containsValue(renamedName)) {
							boolean renamed = false;
							for (String var : varsInState) {
								if (!batchRename.containsValue(var)) {
									renamed = true;
									renameMap.put(defPreq.getName().getIdentifier(), var);
									break;
								}

							}
							if (renamed == false) {
								if (batchRename.containsKey(renamedName))
									renameMap.put(renamedName, batchRename.get(renamedName));
								else {
									Settings.consoleLogger.error(String.format("renaming didn't happend for  %s--%s",
											stmt.getName(), stmt.statement.toString()));
									BackwardTestMerger.mergingResult.fatalError = true;
								}

							}
						}
					}
				}

			}
		}
	}

	public static void populateStateField(Collection<TestStatement> path) {
		// file path to list of statements
		Map<String, List<TestStatement>> fileTestStatementMapping = new HashMap<String, List<TestStatement>>();
		for (TestStatement stmt : path) {

			String testCaseStr = getTestCaseName(stmt);
			String filePath = Utils.getTestCaseFile(testCaseStr);
			Utils.addToTheListInMap(fileTestStatementMapping, filePath, stmt);
		}

		for (Entry<String, List<TestStatement>> entry : fileTestStatementMapping.entrySet()) {
			String filePath = entry.getKey();
			List<TestStatement> statements = entry.getValue();

			populateForFile(filePath, statements);
		}

	}

	private static String getTestCaseName(TestStatement stmt) {
		String stmtStr = stmt.getName();
		int index = stmtStr.lastIndexOf('-');
		String testCaseStr = stmtStr.substring(0, index);
		return testCaseStr;
	}

	public static void populateForFile(String filePath, List<TestStatement> statements) {
		try {
			String source = FileUtils.readFileToString(filePath);
			Document document = new Document(source);
			List<ClassModel> classes = ClassModel.getClasses(document.get(), true, filePath,
					new String[] { Settings.PROJECT_PATH }, new String[] { Settings.LIBRARY_JAVA });

			for (TestStatement stmt : statements) {
				String stmtMethodName = Utils.getTestCaseName(Utils.getTestCaseNameFromTestStatement(stmt.getName()));
				for (ClassModel clazz : classes) {
					List<Method> methods = clazz.getMethods();

					for (Method m : methods) {

						String methodName = m.getMethodDec().getName().toString();
						if (methodName.equals(stmtMethodName)) {
							// if (methodName.equals("testSetSubMatrix") &&
							// filePath.contains("Array2DRowRealMatrixTest"))
							// System.out.println();
							List stmts = m.getMethodDec().getBody().statements();
							int index = getTestStatementNumber(stmt.getName());
							if (0 <= index && index < stmts.size())
								stmt.statement = (Statement) stmts.get(index);
							// StatementNumberingVisitor snv = new
							// StatementNumberingVisitor();
							// m.getMethodDec().accept(snv);
							// if (0 <= index && index < snv.statements.size())
							// stmt.statement = snv.statements.get(index);
							break;
						}
					}
				}
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static int getTestStatementNumber(String xmlTestStatementStr) {
		int index = xmlTestStatementStr.lastIndexOf('-');
		int endIndex = xmlTestStatementStr.lastIndexOf('.');
		return Integer.valueOf(xmlTestStatementStr.substring(index + 1, endIndex));
	}

}
