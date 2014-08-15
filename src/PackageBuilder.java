import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

public class PackageBuilder
 {
    private static final Map<String, String> extToPackageName;

    static
    {
        Map<String, String> tmpMap = new HashMap<String, String>();
        tmpMap.put("cls", "ApexClass");
        tmpMap.put("trigger", "ApexTrigger");
        tmpMap.put("page", "ApexPage");
        tmpMap.put("component", "ApexComponent");
        tmpMap.put("resource", "StaticResource");
        extToPackageName = Collections.unmodifiableMap(tmpMap);
    }

    private void createXML(Map<String, ArrayList<String>> fileMap, StreamResult result)
    {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder;
        try {
            docBuilder = docFactory.newDocumentBuilder();

            Document doc = docBuilder.newDocument();
            Element root = doc.createElement("Package");
            doc.appendChild(root);

            for(String key : fileMap.keySet())
            {
                Element types = doc.createElement("types");
                root.appendChild(types);

                for(String name : fileMap.get(key))
                {
                    Element member = doc.createElement("members");
                    member.appendChild(doc.createTextNode(name));
                    types.appendChild(member);
                }

                Element name = doc.createElement("name");
                name.appendChild(doc.createTextNode(this.getPackageName(key)));
                types.appendChild(name);
            }

            Element version = doc.createElement("version");
            version.appendChild(doc.createTextNode("29.0"));
            root.appendChild(version);

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer;

            transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            DOMSource source = new DOMSource(doc);
            transformer.transform(source, result);
        }
        catch (ParserConfigurationException e)
        {
            e.printStackTrace();
        }
        catch (TransformerConfigurationException e)
        {
            e.printStackTrace();
        }
        catch (TransformerException e)
        {
            e.printStackTrace();
        }
    }

    private void createXML(Map<String, ArrayList<String>> fileMap, File file)
    {
        this.createXML(fileMap, new StreamResult(file));
    }

    private void createXML(Map<String, ArrayList<String>> fileMap, OutputStream stream)
    {

        this.createXML(fileMap, new StreamResult(stream));
    }

    private void createXML(Map<String, ArrayList<String>> fileMap)
    {
        this.createXML(fileMap, new File("package.xml"));
    }

    private String getPackageName(String name)
    {
        if(extToPackageName.containsKey(name))
        {
            return extToPackageName.get(name);
        }
        return "";
    }

    private String getFileContents(String fileName)
    {
        BufferedReader br;
        String contents = null;

        if(fileName == null)
            return null;

        try
        {
            br = new BufferedReader(new FileReader(fileName));
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null)
            {
                sb.append(line);
                sb.append('\n');
                line = br.readLine();
            }

            br.close();

            contents = sb.toString();
        }
        catch (IOException e)
        {
            System.out.println("Error reading file");
            e.printStackTrace();
        }
       return contents;
    }

    private Map<String, ArrayList<String>> getMapFromContents(String contents)
    {
        String[] packageArr = contents.split("\n");
        Map<String, ArrayList<String>> extToFileName = new HashMap<String, ArrayList<String>>();

        for(String str : packageArr)
        {
            int slashIndex = str.lastIndexOf("/");
            int dotIndex = str.lastIndexOf(".");
            
            if( dotIndex == -1 )
                continue;
            String fileName = str.substring(slashIndex+1, dotIndex);
            String extName = str.substring(dotIndex+1, str.length());

            if(!extToFileName.containsKey(extName) && extToPackageName.containsKey(extName))
            {
                extToFileName.put(extName, new ArrayList<String>());
            }
            if(extToPackageName.containsKey(extName))
                extToFileName.get(extName).add(fileName);
        }

        return extToFileName;
    }

    public static void main(String[] args)
    {
        String fromFile = null;
        String toFile = null;
        if(args != null)
        {
            if(args.length == 1)
            {
                fromFile = args[0];
            }
            else if(args.length == 2)
            {
                fromFile = args[0];
                toFile = args[1];
            }
        }

        PackageBuilder pb = new PackageBuilder();

        String contents = pb.getFileContents(fromFile);

        if(contents == null)
        {
            System.out.println("Error: Must supply valid file name as argument");
            return;
        }

        Map<String, ArrayList<String>> extToFileName = pb.getMapFromContents(contents);

        if(toFile == null)
        {
            pb.createXML(extToFileName);
        }
        else if(toFile.equals("console"))
        {
            pb.createXML(extToFileName, System.out);
        }
        else
        {
            pb.createXML(extToFileName, new File(args[1]));
        }
    }
}