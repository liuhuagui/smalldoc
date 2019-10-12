package com.github.liuhuagui.smalldoc.properties;


import java.util.List;

public class SmallDocProperties {
    private boolean enabled;

    private String urlPattern;
    /**
     * project name
     */
    private String projectName;

    /**
     * source code absolute paths
     *
     * @see <a href="https://docs.oracle.com/javase/8/docs/technotes/tools/windows/javadoc.html#CHDEHCDG">https://docs.oracle.com/javase/8/docs/technotes/tools/windows/javadoc.html#CHDEHCDG</a>
     */
    private List<String> sourcePaths;

    /**
     * Generates documentation from source files in the specified packages and recursively in their subpackages.
     *
     * @see <a href="https://docs.oracle.com/javase/8/docs/technotes/tools/windows/javadoc.html#CHDJEDJI">https://docs.oracle.com/javase/8/docs/technotes/tools/windows/javadoc.html#CHDJEDJI</a>
     */
    private List<String> packages;

    private List<String> libraryTypePackages;

    private List<String> libraryTypeQualifiedNames;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getUrlPattern() {
        return urlPattern;
    }

    public void setUrlPattern(String urlPattern) {
        this.urlPattern = urlPattern;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public List<String> getSourcePaths() {
        return sourcePaths;
    }

    public void setSourcePaths(List<String> sourcePaths) {
        this.sourcePaths = sourcePaths;
    }

    public List<String> getPackages() {
        return packages;
    }

    public void setPackages(List<String> packages) {
        this.packages = packages;
    }

    public List<String> getLibraryTypePackages() {
        return libraryTypePackages;
    }

    public void setLibraryTypePackages(List<String> libraryTypePackages) {
        this.libraryTypePackages = libraryTypePackages;
    }

    public List<String> getLibraryTypeQualifiedNames() {
        return libraryTypeQualifiedNames;
    }

    public void setLibraryTypeQualifiedNames(List<String> libraryTypeQualifiedNames) {
        this.libraryTypeQualifiedNames = libraryTypeQualifiedNames;
    }
}
