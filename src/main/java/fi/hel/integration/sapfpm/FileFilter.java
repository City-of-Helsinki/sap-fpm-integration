package fi.hel.integration.sapfpm;

import io.quarkus.runtime.annotations.RegisterForReflection;
import io.smallrye.common.annotation.Identifier;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Singleton;
import org.apache.camel.Exchange;
import org.apache.camel.component.file.GenericFile;
import org.apache.camel.component.file.GenericFileFilter;

@Singleton
@Identifier("fileFilter")
@RegisterForReflection
public class FileFilter implements GenericFileFilter {

    public String fileToExclude;
    public String fileFilterTxt;

    public void setFileToExlude(Exchange e) {
        this.fileToExclude = e.getMessage().getHeader("CamelFileName", String.class);
        System.out.println("set file to exlcud" + fileToExclude);
        this.fileFilterTxt = e.getProperty("fileFilterTxt", String.class);
    }

    @Override
    public boolean accept(GenericFile file) {
        System.out.println("accept: " + file.getAbsoluteFilePath() + "/" + file.getFileName() + "<-> " + this.fileToExclude + " filterTxt: " +  this.fileFilterTxt);
        System.out.println("accepted: " + (!this.fileToExclude.equals(file.getFileName()) && file.getFileName().matches(fileFilterTxt)));
        return !this.fileToExclude.equals(file.getFileName()) && file.getFileName().matches(fileFilterTxt);
    }
}
