import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.*;
import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class DigestCalculator {

    private static String digestType;
    private static String pathToFolderWithFiles;
    private static String pathToFileWithDigestList;

    private static List<FileDigest> fileDigests;
    private static List<FileDigest> newFilesDigests;

    public static void readDigestList() throws ParserConfigurationException, SAXException, IOException{
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

        try {
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new File(pathToFileWithDigestList));
            doc.getDocumentElement().normalize();

            NodeList entryList = doc.getElementsByTagName("FILE_ENTRY");

            for(int i=0; i<entryList.getLength(); i++){
                Node entryNode = entryList.item(i);

                if(entryNode.getNodeType() == Node.ELEMENT_NODE){
                    Element fileElement = (Element) entryNode;

                    String fileName = fileElement.getElementsByTagName("FILE_NAME").item(0).getTextContent();

                    FileDigest file = new FileDigest(fileName);

                    NodeList digestList = fileElement.getElementsByTagName("DIGEST_ENTRY");
                    
                    for(int j=0; j<digestList.getLength(); j++){
                        Node digestNode = digestList.item(j);

                        if(digestNode.getNodeType() == Node.ELEMENT_NODE){
                            Element digestElement = (Element) digestNode;

                            String digestType = digestElement.getElementsByTagName("DIGEST_TYPE").item(0).getTextContent();
                            String digestHex = digestElement.getElementsByTagName("DIGEST_HEX").item(0).getTextContent();
                        
                            file.addDigestType(digestType, digestHex);
                        }
                    }
                    fileDigests.add(file);
                }
            }
        }catch (ParserConfigurationException | SAXException | IOException e) {
            throw e;
        }
    }

    public static byte[] digestMessage(File file) throws IOException, NoSuchAlgorithmException{
        MessageDigest messageDigest = MessageDigest.getInstance(digestType);
        try(
            InputStream fileInputStream = new FileInputStream(file);
            BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
            ){
            byte[] buffer = new byte[1024];
            int bytesRead;
            while((bytesRead = bufferedInputStream.read(buffer)) != -1){
                messageDigest.update(buffer, 0, bytesRead);
            }
        }
        return messageDigest.digest();
    }

    public static String convertToHex(byte[] digest){
        StringBuffer buf = new StringBuffer();
        for(int i = 0; i < digest.length; i++) {
            String hex = Integer.toHexString(0x0100 + (digest[i] & 0x00FF)).substring(1);
            buf.append((hex.length() < 2 ? "0" : "") + hex);
        }
        return buf.toString();
    }

    public static void calculateAllFilesHash() throws FileNotFoundException, IOException, NoSuchAlgorithmException{
        File folder = new File(pathToFolderWithFiles);
        File[] files = folder.listFiles();
        if(files == null){
            System.out.println("ERRO! A pasta não existe ou não pode ser acessada");
            throw new FileNotFoundException();
        }
        
        for(File file : files){
            try{
                if (file.isFile()) {
                    byte[] hash = digestMessage(file);
                    String hexHash = convertToHex(hash);

                    FileDigest fileDigest = new FileDigest(file.getName());
                    fileDigest.addDigestType(digestType, hexHash);

                    newFilesDigests.add(fileDigest);
                }
            }catch(FileNotFoundException e){
                System.out.println("ERRO! O arquivo "+pathToFolderWithFiles+" não foi encontrado.");
            }catch( IOException e){
                System.out.println("ERRO processando o arquivo: "+file.getName());
                throw e;
            }catch( NoSuchAlgorithmException e){
                System.out.println("ERRO! O algoritmo passado como argumento não é válido");
                throw e;
            }
        }
    }

    public static void checkAndSaveDigests(){
        for(FileDigest newFile : newFilesDigests){
            String status = "";
            for(FileDigest f : newFilesDigests){
                if(!newFile.getFileName().equals(f.getFileName()) && newFile.getDigestHexByType(digestType).equals(f.getDigestHexByType(digestType))){
                    status = "COLISION";
                }
            }
            if(!status.equals("COLISION")){
                status = FileDigest.checkFileDigestStatus(fileDigests, newFile, digestType);
            }
            if(status.equals("NOT FOUND")){
                FileDigest.addNewDigestToFileDigestList(fileDigests, newFile, digestType);
            }

            System.out.println(newFile.getFileName()+" "+digestType+newFile.getDigestHexByType(digestType)+" "+"("+status+")");
        }
    }

    public static void saveXMLFile() throws Exception{
        try{
            FileWriter writer = new FileWriter(pathToFileWithDigestList);

            writer.write("<CATALOG>\n");
            for(FileDigest f:fileDigests){
                writer.write("    <FILE_ENTRY>\n");
                writer.write("        <FILE_NAME>"+f.getFileName()+"</FILE_NAME>\n");

                for(FileDigest.DigestType digestType : f.getDigestTypes()){
                    writer.write("        <DIGEST_ENTRY>\n");
                    writer.write("            <DIGEST_TYPE>"+digestType.getType()+"</DIGEST_TYPE>\n");
                    writer.write("            <DIGEST_HEX>"+digestType.getHex()+"</DIGEST_HEX>\n");
                    writer.write("        </DIGEST_ENTRY>\n");
                }

                writer.write("    </FILE_ENTRY>\n");
            }

            writer.write("</CATALOG>");
            writer.close();

            System.out.println("Novo CATALOGO salvo com sucesso!");
        }catch(Exception e){
            System.out.println("ERRO salvando o novo CATALOGO!");
            throw e;
        }
    }

    public static void main(String[] args) {

        if (args.length != 3) {
            System.out.println("ERRO! A entrada do programa deve ser da seguinte forma: DigestCalculator <Tipo_Digest> <Caminho_da_Pasta_dos_Arquivos> <Caminho_ArqListaDigest>");
            return;
        }
        
        digestType = args[0];
        pathToFolderWithFiles = args[1];
        pathToFileWithDigestList = args[2];
        fileDigests = new ArrayList<>();
        newFilesDigests = new ArrayList<>();

        System.out.println("Tipo de Digest: " + digestType);
        System.out.println("Caminho da Pasta dos Arquivos: " + pathToFolderWithFiles);
        System.out.println("Caminho do Arquivo de Lista de Digest: " + pathToFileWithDigestList);
    
        try{
            readDigestList();
        }catch(Exception e){
            e.printStackTrace();
            return;
        }
        FileDigest.printFileDigestList(fileDigests);

        try{
            calculateAllFilesHash();
        }catch(Exception e){
            return;
        }
        FileDigest.printFileDigestList(newFilesDigests);

        checkAndSaveDigests();

        try{
            saveXMLFile();
        }catch(Exception e){
            return;
        }
    }
}
