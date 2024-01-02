import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class Main {

    static void aplicarComando(File file, String comando) throws IOException, InterruptedException {
        String svnRevertCommand = comando + file.getAbsoluteFile();
        // Construir o processo
        ProcessBuilder processBuilder = new ProcessBuilder("cmd", "/c", svnRevertCommand);
        processBuilder.redirectErrorStream(true);
        // Iniciar o processo
        Process process = processBuilder.start();
        process.waitFor();
    }

    static void buscarArquivos(File arquivoRaiz, List<File> listaArquivos) {
        if (arquivoRaiz == null) {
            return;
        }
        if (arquivoRaiz.isFile()) {
            listaArquivos.add(arquivoRaiz);
            return;
        }

        File[] arquivosAtuais = arquivoRaiz.listFiles();
        if (arquivosAtuais == null) {
            return;
        }
        for (File arquivo : arquivosAtuais) {
            if (arquivo.isDirectory()) {
                buscarArquivos(arquivo, listaArquivos);
            }
            listaArquivos.add(arquivo);
        }
    }

    static boolean verificarDiferenca(File file) {
        try {
            String svnRevertCommand = "svn diff " + file.getAbsoluteFile();
            // Construir o processo
            ProcessBuilder processBuilder = new ProcessBuilder("cmd", "/c", svnRevertCommand);
            processBuilder.redirectErrorStream(true);

            // Iniciar o processo
            Process process = processBuilder.start();

            // Ler a saída do processo e adicionar as linhas do comando
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder linhas = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                linhas.append(line).append("\n");
            }
            process.waitFor();

            if (linhas.toString().isEmpty()) {
                return false;
            }

            // Linhas com alteração
            String[] linhasDeCodigoArr = linhas.toString().split("@@\n");
            if (linhasDeCodigoArr.length < 2) {
                return false;
            }
            String linhasDeCodigo = linhas.toString().split("@@\n")[1];

            StringBuilder linhasRemoto = new StringBuilder();
            StringBuilder linhasLocal = new StringBuilder();

            for (String linha : linhasDeCodigo.split("\n")) {
                if (linha.startsWith("-")) {
                    linhasRemoto.append(linha.replaceAll("\\s+", "").replaceFirst("-", ""));
                } else {
                    linhasLocal.append(linha.replaceAll("\\s+", "").replaceFirst("\\+", ""));
                }
            }
            return linhasLocal.toString().trim().equals(linhasRemoto.toString().trim());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        final String PATH_SRC = "C:\\refiltek\\Projetos\\JSF\\refiltek_producao_svn_teste";
        List<File> listaArquivos = new ArrayList<>();

        buscarArquivos(new File(PATH_SRC), listaArquivos);

        List<File> listaArquivosNaoModificados = new ArrayList<>();
        for (int index = 0; index < listaArquivos.size(); index++) {
            File arquivo = listaArquivos.get(index);
            if (arquivo.getName().endsWith(".java")) {
                if (verificarDiferenca(arquivo)) {
                    listaArquivosNaoModificados.add(arquivo);
                }
            }
            System.out.println("Buscando arquivos: " + index + " | " + listaArquivos.size());
        }

        for (int index = 0; index < listaArquivosNaoModificados.size(); index++) {
            File arquivoNaoModificado = listaArquivosNaoModificados.get(index);
            aplicarComando(arquivoNaoModificado, "svn revert -R ");
            aplicarComando(arquivoNaoModificado, "svn update ");
            System.out.println("Executando comandos: " + index + " | " + listaArquivosNaoModificados.size());
        }

    }
}