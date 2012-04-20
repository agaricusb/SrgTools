package nl.hardijzer.fw.applysrg;
import java.util.zip.*;
import java.io.*;
import java.util.*;

import org.objectweb.asm.*;
import org.objectweb.asm.commons.*;


class Method implements Comparable<Method> {
	public String name;
	public String desc;
	public Method(String strName, String strArguments) {
		this.name=strName;
		this.desc=strArguments;
	}
	
	@Override
	public int compareTo(Method b) {
		int cmpName=name.compareTo(b.name);
		int cmpDesc=desc.compareTo(b.desc);
		if (cmpName!=0) return cmpName;
		return cmpDesc;
	}
}

class MappedClass {
	public String strNewName;
	public Map<String,String> mapFields;
	public Map<Method,String> mapMethods;
	
	public MappedClass(String strNewName) {
		this.strNewName=strNewName;
		this.mapFields=new TreeMap<String,String>();
		this.mapMethods=new TreeMap<Method,String>();
	}
}

class ClassInfo {
	public Set<String> setFields;
	public Set<Method> setMethods;
	public Set<String> setInheritance;
	
	public ClassInfo() {
		setFields=new TreeSet<String>();
		setMethods=new TreeSet<Method>();
		setInheritance=new TreeSet<String>();
	}
}

class MyRemapper extends Remapper {
	public Map<String,MappedClass> mapClasses;
	public Map<String,ClassInfo> mapClassInfo;
	
	public MyRemapper(Map<String,MappedClass> mapClasses, Map<String,ClassInfo> mapInheritance) {
		super();
		this.mapClasses=mapClasses;
		this.mapClassInfo=mapInheritance;
	}
	
	@Override
	public String map(String typeName) {
		MappedClass other=mapClasses.get(typeName);
		//if (other!=null && !other.strNewName.equals(typeName))
		//	System.out.println("Mapping class "+typeName+" to "+other.strNewName);
		if (other!=null)
			return other.strNewName;
		if (typeName.indexOf('/')==-1)
			return "net/minecraft/server/"+typeName;
		return typeName;
	}
	
	public String mapMethodNameDirect(String owner, Method m) {
		MappedClass mappedClass=mapClasses.get(owner);
		if (mappedClass!=null) {
			String strMapped=mappedClass.mapMethods.get(m);
			if (strMapped!=null) {
				//System.out.println("Mapping method "+owner+"/"+m.name+" to new name "+strMapped);
				return strMapped;
			}
		}
		return null;
	}
	
	public String mapFieldNameDirect(String owner, String f) {
		MappedClass mappedClass=mapClasses.get(owner);
		if (mappedClass!=null) {
			String strMapped=mappedClass.mapFields.get(f);
			if (strMapped!=null) {
				//System.out.println("Mapping field "+owner+"/"+f+" to new name "+strMapped);
				return strMapped;
			}
		}
		return null;
	}
	
	public String locateMethod(String owner, Method m) {
		Queue<String> q=new LinkedList<String>();
		q.add(owner);
		while (!q.isEmpty()) {
			String strOwner=q.remove();
			ClassInfo info=mapClassInfo.get(strOwner);
			if (info==null)
				continue;
			for (String inherit : info.setInheritance)
				q.add(inherit);
			if (mapClasses.containsKey(strOwner) && info.setMethods.contains(m))
				return strOwner;
		}
		return null;
	}
	
	public String locateField(String owner, String f) {
		Queue<String> q=new LinkedList<String>();
		q.add(owner);
		while (!q.isEmpty()) {
			String strOwner=q.remove();
			ClassInfo info=mapClassInfo.get(strOwner);
			if (info==null)
				continue;
			for (String inherit : info.setInheritance)
				q.add(inherit);
			if (mapClasses.containsKey(strOwner) && info.setFields.contains(f))
				return strOwner;
		}
		return null;
	}
	
	@Override
	public String mapMethodName(String owner, String name, String desc) {
		if (owner.equals("railcraft/common/carts/EntityCartTNT") && name.equals("c") && desc.equals("()I")) {
			System.out.println("mapMethodName: "+owner+" "+name+" "+desc);
		}
		Method m=new Method(name,desc);
		String strActualOwner=locateMethod(owner,m);
		//if (strActualOwner!=null)
		//	System.out.println("Tracked method "+owner+"/"+name+" "+desc+" to "+strActualOwner);
		String strMapped=mapMethodNameDirect((strActualOwner!=null)?strActualOwner:owner,m);
		return (strMapped==null)?name:strMapped;
	}
	
	@Override
	public String mapFieldName(String owner, String name, String desc) {
		String strActualOwner=locateField(owner,name);
		//if (strActualOwner!=null)
		//	System.out.println("Tracked field "+owner+"/"+name+" "+desc+" to "+strActualOwner);
		String strMapped=mapFieldNameDirect((strActualOwner!=null)?strActualOwner:owner,name);
		return (strMapped==null)?name:strMapped;
	}
}

class InheritanceMapClassVisitor implements ClassVisitor {
	public String strName;
	public ClassInfo info;
	
	public InheritanceMapClassVisitor() {
		strName="";
		info=new ClassInfo();
	}
	
	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		System.out.println(name+":");
		this.strName=name;
		this.info.setInheritance.add(superName);
		for (int i=0; i<interfaces.length; i++)
			this.info.setInheritance.add(interfaces[i]);
	}

	@Override
	public AnnotationVisitor visitAnnotation(String arg0, boolean arg1) {
		return null;
	}

	@Override
	public void visitAttribute(Attribute arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visitEnd() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public FieldVisitor visitField(int access, String name, String desc,
			String signature, Object value) {
		this.info.setFields.add(name);
		return null;
	}

	@Override
	public void visitInnerClass(String arg0, String arg1, String arg2, int arg3) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc,
			String signature, String[] exceptions) {
		this.info.setMethods.add(new Method(name,desc));
		return null;
	}

	@Override
	public void visitOuterClass(String arg0, String arg1, String arg2) {
		// TODO Auto-generated method stub
	}

	@Override
	public void visitSource(String arg0, String arg1) {
		// TODO Auto-generated method stub
	}
	
}

public class ApplySrg {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		String strInputFilename=null;
		String strOutputFilename=null;
		List<String> listInputSrg=new LinkedList<String>();
		List<String> listInputInheritance=new LinkedList<String>();
		String strOrphanPackage="";
		for (int i=0; i<args.length; i++) {
			if (args[i].equals("--srg"))
				listInputSrg.add(args[++i]);
			else if (args[i].equals("--inheritance"))
				listInputInheritance.add(args[++i]);
			else if (args[i].equals("--orphanpackage"))
				strOrphanPackage=args[++i];
			else if (args[i].equals("--in"))
				strInputFilename=args[++i];
			else if (args[i].equals("--out"))
				strOutputFilename=args[++i];
		}
		if (strInputFilename==null || strOutputFilename==null) {
			System.err.println("Usage: java -jar ApplySrg.jar [options]");
			System.err.println("Options:");
			System.err.println("--srg <srg file>\tLoads the SRG file");
			System.err.println("--inheritance <jar/zip>\tLoads inheritance map from jar");
			System.err.println("--orphanpackage <package name>\tPuts all orphan classes into this package");
			System.err.println("--in <jar/zip>");
			System.err.println("--out <jar/zip>");
			return;
		}
		
		Map<String,MappedClass> mapClasses=new TreeMap<String,MappedClass>();
		for (String srg : listInputSrg) {
			System.out.println("Reading SRG: "+srg);
			BufferedReader brSrg = new BufferedReader(new FileReader(srg));
			String strLine;
			while ((strLine=brSrg.readLine())!=null) {
				String arrLine[]=strLine.split(" ");
				if (arrLine[0].equals("PK:")) {
					//Ignore package specification
				} else if (arrLine[0].equals("CL:")) {
					String strFrom=arrLine[1];
					String strTo=arrLine[2];
					MappedClass mappedCurrent=mapClasses.get(strFrom);
					if (mappedCurrent==null) {
						mapClasses.put(strFrom,new MappedClass(strTo));
					} else {
						if (!mappedCurrent.strNewName.equals(strTo)) {
							System.err.println("ERROR: Mismatching mappings found");
							return;
						}
					}
				} else if (arrLine[0].equals("FD:")) {
					String strFrom=arrLine[1];
					String strTo=arrLine[2];
					int nSplitFrom=strFrom.lastIndexOf('/');
					int nSplitTo=strTo.lastIndexOf('/');
					if (nSplitFrom==-1 || nSplitTo==-1) {
						System.err.println("ERROR: Invalid field specification");
						return;
					}
					String strFromClass=strFrom.substring(0,nSplitFrom);
					strFrom=strFrom.substring(nSplitFrom+1);
					String strToClass=strTo.substring(0,nSplitTo);
					strTo=strTo.substring(nSplitTo+1);
					MappedClass mappedCurrent=mapClasses.get(strFromClass);
					if (strFromClass.equals(strToClass) && mappedCurrent==null) {
						mapClasses.put(strFromClass,mappedCurrent=new MappedClass(strToClass));
					}
					if (mappedCurrent==null || !mappedCurrent.strNewName.equals(strToClass)) {
						System.err.println("ERROR: Class mapping invalid or non-existant on field");
						return;
					}
					mappedCurrent.mapFields.put(strFrom,strTo);
				} else if (arrLine[0].equals("MD:")) {
					String strFrom=arrLine[1];
					String strFromArguments=arrLine[2];
					String strTo=arrLine[3];
					String strToArguments=arrLine[4];
					int nSplitFrom=strFrom.lastIndexOf('/');
					int nSplitTo=strTo.lastIndexOf('/');
					if (nSplitFrom==-1 || nSplitTo==-1) {
						System.err.println("ERROR: Invalid field specification");
						return;
					}
					String strFromClass=strFrom.substring(0,nSplitFrom);
					strFrom=strFrom.substring(nSplitFrom+1);
					String strToClass=strTo.substring(0,nSplitTo);
					strTo=strTo.substring(nSplitTo+1);
					MappedClass mappedCurrent=mapClasses.get(strFromClass);
					if (strFromClass.equals(strToClass) && mappedCurrent==null) {
						mapClasses.put(strFromClass,mappedCurrent=new MappedClass(strToClass));
					}
					if (mappedCurrent==null || !mappedCurrent.strNewName.equals(strToClass)) {
						System.err.println("ERROR: Class mapping invalid or non-existant on field");
						return;
					}
					//NOTE: arguments not saved, will be mapped automagically.
					mappedCurrent.mapMethods.put(new Method(strFrom,strFromArguments),strTo);
				}
			}
		}
		System.out.println("Class map loaded of "+mapClasses.size()+" classes");
		Map<String,ClassInfo> mapClassInheritance=new HashMap<String,ClassInfo>();
		for (String inherit : listInputInheritance) {
			System.out.println("Parsing inheritance in "+inherit);
			ZipFile zipInherit=new ZipFile(inherit);
			Enumeration<? extends ZipEntry> entries=zipInherit.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry=entries.nextElement();
				if (entry.isDirectory())
					continue;
				if (entry.getName().endsWith(".class")) {
					ClassReader cr=new ClassReader(zipInherit.getInputStream(entry));
					InheritanceMapClassVisitor cvInheritance=new InheritanceMapClassVisitor();
					cr.accept(cvInheritance,0);
					mapClassInheritance.put(cvInheritance.strName,cvInheritance.info);
				}
			}
			zipInherit.close();
		}
		System.out.println("Inheritance map loaded of "+mapClassInheritance.size()+" classes");

		ZipFile zipInput=new ZipFile(strInputFilename);
		
		ZipOutputStream zipOutput=new ZipOutputStream(new FileOutputStream(strOutputFilename));
		Enumeration<? extends ZipEntry> entries=zipInput.entries();
		while (entries.hasMoreElements()) {
			ZipEntry entry=entries.nextElement();
			if (entry.isDirectory())
				continue;
			if (entry.getName().endsWith(".class")) {
				ClassReader cr=new ClassReader(zipInput.getInputStream(entry));
				ClassWriter cw=new ClassWriter(0);
				Remapper remapper=new MyRemapper(mapClasses,mapClassInheritance);
				RemappingClassAdapter ca=new RemappingClassAdapter(cw,remapper);
				cr.accept(ca,ClassReader.EXPAND_FRAMES);
				byte[] bOutput=cw.toByteArray();
				
				ZipEntry entryCopy=new ZipEntry((entry.getName().indexOf('/')==-1?strOrphanPackage:"")+entry.getName());
				entryCopy.setCompressedSize(-1);
				entryCopy.setSize(bOutput.length);
				zipOutput.putNextEntry(entryCopy);
				zipOutput.write(bOutput);
				zipOutput.closeEntry();
			} else {
				ZipEntry entryCopy=new ZipEntry(entry);
				entryCopy.setCompressedSize(-1);
				zipOutput.putNextEntry(entryCopy);
				InputStream is=zipInput.getInputStream(entry);
				byte[] buffer=new byte[1024];
				int read=0;
				while ((read=is.read(buffer))!=-1)
					zipOutput.write(buffer,0,read);
				zipOutput.closeEntry();
			}
		}
		zipInput.close();
		zipOutput.close();
		System.out.println("Done!");
	}

}