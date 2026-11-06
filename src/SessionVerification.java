import com.github.bhlangonijr.chesslib.*;
import java.nio.file.*;
import java.util.*;

public final class SessionVerification {
    private static void check(boolean condition,String message) { if(!condition) throw new AssertionError(message); }
    public static void main(String[] args) throws Exception {
        Path folder=Files.createTempDirectory("session-replacement-");
        try {
            Path path=folder.resolve("sessao.properties");
            var store=new SessionStore(path);
            store.save(Side.WHITE,"sem-limite",List.of());
            Board board=new Board();
            var move=TerminalGame.parseMove(board,"e2e4");
            for(int i=0;i<20;i++) store.save(Side.WHITE,"sem-limite",List.of(move));
            check(store.load().moves().size()==1,"Substituições reais sucessivas");
            check(store.load().increment()==0,"Sessão nova preserva ausência de incremento");
            Path legacy=folder.resolve("legado.properties");
            Files.writeString(legacy,"version=1\ncolor=WHITE\nclock=15:00\nmoves=\n");
            check(new SessionStore(legacy).load().increment()==10_000,"Sessões antigas conservam incremento original");
            int[] attempts={0};
            var windows=new SessionStore(path,(from,to,atomic) -> {
                if(atomic) throw new FileAlreadyExistsException(to.toString());
                if(attempts[0]++==0) throw new AccessDeniedException(to.toString());
                Files.move(from,to,StandardCopyOption.REPLACE_EXISTING);
            });
            windows.save(Side.BLACK,"sem-limite",List.of(move));
            check(store.load().color()==Side.BLACK && attempts[0]==2,"Fallback e nova tentativa após bloqueio transitório");
            var blocked=new SessionStore(path,(from,to,atomic) -> { throw new AccessDeniedException(to.toString()); });
            boolean failed=false;
            try { blocked.save(Side.WHITE,"sem-limite",List.of()); }
            catch(java.io.IOException expected) { failed=expected.getMessage().contains("Cópia recuperável"); }
            check(failed && store.load().color()==Side.BLACK,"Falha deve preservar a sessão anterior");
            try(var files=Files.list(folder)) {
                Path recovery=files.filter(p -> p.toString().endsWith(".tmp")).findFirst().orElseThrow();
                check(new SessionStore(recovery).load().color()==Side.WHITE,"Cópia recuperável deve conter o snapshot novo completo");
            }
            System.out.println("OK: substituições sucessivas, fallback Windows, bloqueio transitório e preservação de recuperação.");
        } finally { try(var files=Files.list(folder)) { for(Path file:files.toList()) Files.delete(file); } Files.delete(folder); }
    }
}
