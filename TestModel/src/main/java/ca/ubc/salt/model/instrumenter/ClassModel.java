package ca.ubc.salt.model.instrumenter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import ca.ubc.salt.model.utils.Utils;

public class ClassModel {
	public TypeDeclaration typeDec = null;
	CompilationUnit cu = null;
	List<FieldDeclaration> staticFields = null;
	List<FieldDeclaration> fields = null;
	List<FieldDeclaration> allFields = null;
	List<Method> methods = null;
	public String name;

	public List<ClassModel> getAllSuperModelsAndThis() {
		List<String> parents = Utils.getAllParents(name);

		List<ClassModel> parentModels = Utils.getAllClasses(parents);
		parentModels.add(0, this);
		return parentModels;
	}

	public boolean isClassIsRunBy(String runWith, String runner) {
		List modifs = this.getTypeDec().modifiers();

		for (Object obj : modifs) {
			if (obj instanceof SingleMemberAnnotation) {
				SingleMemberAnnotation mod = (SingleMemberAnnotation) obj;
				String typeName = mod.getTypeName().getFullyQualifiedName();
				String value = mod.getValue().toString();
				if (typeName.contains(runWith) && value.contains(runner))
					return true;
				;

			} else if (obj instanceof NormalAnnotation) {
				NormalAnnotation mod = (NormalAnnotation) obj;
				String typeName = mod.getTypeName().getFullyQualifiedName();
				String value = mod.values().toString();
				if (typeName.contains(runWith) && value.contains(runner))
					return true;
			}
		}
		return false;
	}

	public boolean isInstrumentable() {
		// boolean abstrc = isAbstract();
		//
		//
		//// return !abstrc;
		// return typeDec.getSuperclassType() == null &&
		// typeDec.superInterfaceTypes().isEmpty() && !typeDec.isInterface()
		// && !abstrc;
		if (Instrumenter.parentClassDependency.containsKey(Utils.getTestCaseName(this.name)) == false
				&& !isClassIsRunBy("RunWith", "Retry"))
			return true;
		return false;

	}

	public boolean isAbstract() {
		boolean abstrc = false;
		for (Object obj : typeDec.modifiers()) {
			if (obj instanceof Modifier) {
				Modifier mod = (Modifier) obj;
				if (mod.isAbstract())
					abstrc = true;
			}
		}
		return abstrc;
	}

	public ClassModel(TypeDeclaration typeDec, CompilationUnit cu) throws IOException {
		this.typeDec = typeDec;
		this.cu = cu;
		this.name = typeDec.resolveBinding().getQualifiedName();
	}

	public static List<ClassModel> getClasses(String source) throws IOException {
		return getClasses(source, false, null, null, null);
	}

	public static List<ClassModel> getClasses(String source, boolean binding, String unitName, String[] sources,
			String[] classPath) throws IOException {

		ASTParser parser = ASTParser.newParser(AST.JLS8);

		if (binding)
			parser.setResolveBindings(true);

		parser.setKind(ASTParser.K_COMPILATION_UNIT);

		if (binding)
			parser.setBindingsRecovery(true);

		Hashtable<String, String> pOptions = JavaCore.getOptions();
		pOptions.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_8);
		pOptions.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_1_8);
		pOptions.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_8);
		parser.setCompilerOptions(pOptions);

		if (binding) {
			parser.setUnitName(unitName);
			parser.setEnvironment(classPath, sources, null, true);
		}

		parser.setSource(source.toCharArray());
		CompilationUnit cu = (CompilationUnit) parser.createAST(null);

		List typeDeclarationList = cu.types();

		List<ClassModel> classes = new ArrayList<ClassModel>();

		for (Object type : typeDeclarationList) {
			// TODO: What about other types
			if (type instanceof TypeDeclaration) {
				classes.add(new ClassModel((TypeDeclaration) type, cu));
			}
		}

		return classes;
	}

	private void initMethods() {

		methods = new ArrayList<Method>();
		MethodDeclaration[] methodList = typeDec.getMethods();

		for (MethodDeclaration m : methodList) {
			// System.out.println(method.getName());
			// for (VariableDeclarationFragment var : visitor.varDecs)
			// System.out.println(" " + var.getName());

			Method method = new Method(m, typeDec.resolveBinding().getQualifiedName().toString(), this);
			methods.add(method);
		}
	}

	private void initFields() {
		FieldDeclaration[] allFields = typeDec.getFields();
		this.fields = new ArrayList<FieldDeclaration>();
		for (FieldDeclaration field : allFields) {
			if (!Modifier.isStatic(field.getModifiers())) {
				this.fields.add(field);
			}
		}

	}

	private void initStaticFields() {
		FieldDeclaration[] fields = typeDec.getFields();
		staticFields = new ArrayList<FieldDeclaration>();
		for (FieldDeclaration field : fields) {
			if (Modifier.isStatic(field.getModifiers())) {
				staticFields.add(field);
			}
		}
	}

	public TypeDeclaration getTypeDec() {
		return typeDec;
	}

	public void setTypeDec(TypeDeclaration typeDec) {
		this.typeDec = typeDec;
	}

	public CompilationUnit getCu() {
		return cu;
	}

	public void setCu(CompilationUnit cu) {
		this.cu = cu;
	}

	public List<FieldDeclaration> getStaticFields() {
		if (staticFields == null)
			initStaticFields();
		return staticFields;
	}

	public void setStaticFields(List<FieldDeclaration> staticFields) {
		if (staticFields == null)
			initStaticFields();
		this.staticFields = staticFields;
	}

	public List<Method> getMethods() {
		if (methods == null)
			initMethods();
		return methods;
	}

	public void setMethods(List<Method> methods) {
		this.methods = methods;
	}

	public List<FieldDeclaration> getFields() {
		if (fields == null)
			initFields();
		return fields;
	}

	public void setFields(List<FieldDeclaration> fields) {
		this.fields = fields;
	}

	public List<VariableDeclarationFragment> getVarDecsOfFields() {
		List<VariableDeclarationFragment> vars = new ArrayList<VariableDeclarationFragment>();
		for (FieldDeclaration fd : this.getAllFields()) {
			for (Object obj : fd.fragments()) {
				VariableDeclarationFragment vdf = (VariableDeclarationFragment) obj;
				vars.add(vdf);
			}
		}
		return vars;
	}

	public List<FieldDeclaration> getAllFields() {
		if (this.allFields == null) {
			this.allFields = new LinkedList<FieldDeclaration>();
			this.allFields.addAll(this.getFields());
			this.allFields.addAll(this.getStaticFields());

		}

		return this.allFields;
	}
}
