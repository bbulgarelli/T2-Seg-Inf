// Bernardo Bulgarelli - 2010468
// Camila Perez Aguiar - 1521516

import java.util.ArrayList;
import java.util.List;

public class FileDigest {

    private String fileName;
    private List<DigestType> digestTypes;

    public FileDigest(String fileName){
        this.fileName = fileName;
        this.digestTypes = new ArrayList<>();
    }

    public String getFileName(){
        return this.fileName;
    }

    public List<DigestType> getDigestTypes(){
        return this.digestTypes;
    }

    public void addDigestType(String type, String hex){
        this.digestTypes.add(new DigestType(type, hex));
    }

    public String getDigestHexByType(String type){
        for(DigestType digestType : digestTypes){
            if(digestType.getType().equals(type)){
                return digestType.getHex();
            }
        }
        return "";
    }

    public static void printFileDigestList(List<FileDigest> fileDigestList){
        System.out.println("\n=============== CATALOG ===============");
        for(FileDigest fileDigest : fileDigestList){
            System.out.println("\nFile name: "+fileDigest.fileName);
            System.out.println("Digest Types:");
            for(DigestType digestType : fileDigest.getDigestTypes()){
                System.out.println("    Type: "+digestType.type);
                System.out.println("    Hex digest: "+digestType.hex);
            }
            System.out.println("");
        }
        System.out.println("=======================================\n");
    }

    public static String checkFileDigestStatus(List<FileDigest> fileDigestList, FileDigest newFile, String digestType){
        String status = "NOT FOUND";
        for(FileDigest f : fileDigestList){
            String fdigest = f.getDigestHexByType(digestType);
            if(newFile.fileName.equals(f.fileName) && newFile.getDigestHexByType(digestType).equals(fdigest)){
                status = "OK";
            }

            if(newFile.fileName.equals(f.fileName) && !newFile.getDigestHexByType(digestType).equals(fdigest) && !fdigest.equals("")){
                status = "NOT OK";
            }

            if(!newFile.fileName.equals(f.fileName) && newFile.getDigestHexByType(digestType).equals(fdigest)){
                status = "COLISION";
                return status;
            }
        }
        return status;
    }

    public static void addNewDigestToFileDigestList(List<FileDigest> fileDigestList, FileDigest newDigest, String digestType){
        for(FileDigest f : fileDigestList){
            if(f.fileName.equals(newDigest.fileName)){
                f.addDigestType(digestType, newDigest.getDigestHexByType(digestType));
                return;
            }
        }
        fileDigestList.add(newDigest);
    }

    public static class DigestType{
        private String type;
        private String hex;

        public DigestType(String type, String hex){
            this.type = type;
            this.hex = hex;
        }

        public String getType(){
            return type;
        }

        public String getHex(){
            return hex;
        }
    }
}

