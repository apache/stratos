package org.wso2.carbon.adc.mgt.utils;

import com.gitblit.Constants;
import com.gitblit.models.RepositoryModel;
import com.gitblit.utils.RpcUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.storage.file.FileRepository;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.wso2.carbon.adc.mgt.dao.Repository;
import org.wso2.carbon.adc.mgt.exception.ADCException;
import org.wso2.carbon.adc.mgt.service.RepositoryInfoBean;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.UUID;

public class RepositoryCreator implements Runnable {

	private static final Log log = LogFactory.getLog(RepositoryCreator.class);
	private RepositoryInfoBean repoInfoBean;

	public RepositoryCreator(RepositoryInfoBean repoInfoBean) {
		this.repoInfoBean = repoInfoBean;
	}

	@Override
	public void run() {

		if (repoInfoBean != null) {
			try {
				createRepository(repoInfoBean.getCartridgeAlias(), repoInfoBean.getTenantDomain(),
				                 repoInfoBean.getUserName(), repoInfoBean.getPassword());
				createGitFolderStructure(repoInfoBean.getTenantDomain(),
				                         repoInfoBean.getCartridgeAlias(),
				                         repoInfoBean.getDirArray());

			} catch (Exception e) {
				log.error(e);
			}
		}
	}

    //Creating the internal repo in the same thread as subscribe()
    public void createInternalRepository () throws Exception {

        if (repoInfoBean != null) {
            try {
                createRepository(repoInfoBean.getCartridgeAlias(), repoInfoBean.getTenantDomain(),
                        repoInfoBean.getUserName(), repoInfoBean.getPassword());
                
				if (repoInfoBean.getDirArray() != null && repoInfoBean.getDirArray().length > 0) {
					createGitFolderStructure(repoInfoBean.getTenantDomain(),
					                         repoInfoBean.getCartridgeAlias(),
					                         repoInfoBean.getDirArray());
				}

            } catch (Exception e) {
                String errorMsg = "Creating an internal repository failed for tenant " + repoInfoBean.getTenantDomain();
                log.error(errorMsg, e);
                throw new Exception(errorMsg, e);
            }
        }
    }

	private Repository createRepository(String cartridgeName, String tenantDomain, String userName, String password)
	                                                                                               throws Exception {

		Repository repository = new Repository();
		String repoName = tenantDomain + "/" + cartridgeName;

		try {
			
			log.info("Creating internal repo ["+repoName+"] ");

			RepositoryModel model = new RepositoryModel();
			model.name = repoName;
			model.accessRestriction = Constants.AccessRestrictionType.VIEW;

			char[] passwordArr = password.toCharArray();

			boolean isSuccess =
			                    RpcUtils.createRepository(model,
			                                              "https://localhost:8443/",
			                                              userName, passwordArr);
			if (!isSuccess) {
				throw new Exception("Exception is occurred when creating an internal git repo. ");
			}
		} catch (Exception e) {
			log.error(" Exception is occurred when creating an internal git repo. Reason :" +
			          e.getMessage());
			handleException(e.getMessage(), e);
		}

		return repository;

	}

	private void createGitFolderStructure(String tenantDomain, String cartridgeName,
	                                      String[] dirArray) throws Exception {
		
		if (log.isDebugEnabled()) {
			log.debug("Creating git repo folder structure  ");
		}		
		
		String parentDirName = "/tmp/" + UUID.randomUUID().toString();
		CredentialsProvider credentialsProvider =
		                                          new UsernamePasswordCredentialsProvider(
		                                                                                  System.getProperty(CartridgeConstants.INTERNAL_GIT_USERNAME),
		                                                                                  System.getProperty(CartridgeConstants.INTERNAL_GIT_PASSWORD).toCharArray());
		// Clone
		// --------------------------
		FileRepository localRepo = null;
		try {
			localRepo = new FileRepository(new File(parentDirName + "/.git"));
		} catch (IOException e) {
			log.error("Exception occurred in creating a new file repository. Reason: " + e.getMessage());
			throw e;
		}

		Git git = new Git(localRepo);

		CloneCommand cloneCmd =
		                        git.cloneRepository()
		                           .setURI("https://localhost:8443/git/" + tenantDomain + "/" +
		                                           cartridgeName + ".git")
		                           .setDirectory(new File(parentDirName));

		cloneCmd.setCredentialsProvider(credentialsProvider);
		try {
			log.debug("Clonning git repo");
			cloneCmd.call();
		} catch (Exception e1) {
			log.error("Exception occurred in cloning Repo. Reason: " + e1.getMessage());
			throw e1;
		}
		// ------------------------------------

		// --- Adding directory structure --------

		File parentDir = new File(parentDirName);
		parentDir.mkdir();

		for (String string : dirArray) {
			String[] arr = string.split("=");
			if (log.isDebugEnabled()) {
				log.debug("Creating dir: " + arr[0]);
			}
			File parentFile = new File(parentDirName + "/" + arr[0]);
			parentFile.mkdirs();

			File filess = new File(parentFile, "README");
			String content = "Content goes here";

			filess.createNewFile();
			FileWriter fw = new FileWriter(filess.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(content);
			bw.close();
		}
		// ----------------------------------------------------------

		// ---- Git status ---------------
		StatusCommand s = git.status();
		Status status = null;
		try {
			log.debug("Getting git repo status");
			status = s.call();
		} catch (Exception e) {
			log.error("Exception occurred in git status check. Reason: " + e.getMessage());
			throw e;
		}
		// --------------------------------

		// ---------- Git add ---------------
		AddCommand addCmd = git.add();
		Iterator<String> it = status.getUntracked().iterator();

		while (it.hasNext()) {
			addCmd.addFilepattern(it.next());
		}

		try {
			log.debug("Adding files to git repo");
			addCmd.call();
		} catch (Exception e) {
			log.error("Exception occurred in adding files. Reason: " + e.getMessage());
			throw e;
		}
		// -----------------------------------------

		// ------- Git commit -----------------------

		CommitCommand commitCmd = git.commit();
		commitCmd.setMessage("Adding directories");

		try {
			log.debug("Committing git repo");
			commitCmd.call();
		} catch (Exception e) {
			log.error("Exception occurred in committing . Reason: " + e.getMessage());
			throw e;
		}
		// --------------------------------------------

		// --------- Git push -----------------------
		PushCommand pushCmd = git.push();
		pushCmd.setCredentialsProvider(credentialsProvider);
		try {
			log.debug("Git repo push");
			pushCmd.call();
		} catch (Exception e) {
			log.error("Exception occurred in Git push . Reason: " + e.getMessage());
			throw e;
		}

		try {
			deleteDirectory(new File(parentDirName));
		} catch (Exception e) {
			log.error("Exception occurred in deleting temp files. Reason: " + e.getMessage());
			throw e;
		}

		log.info(" Folder structure  is created ..... ");

	}

	private void handleException(String msg, Exception e) throws Exception {
		log.error(msg, e);
		throw new Exception(msg, e);
	}

	private void deleteDirectory(File file) throws IOException {

		if (file.isDirectory()) {
			// directory is empty, then delete it
			if (file.list().length == 0) {
				file.delete();

			} else {
				// list all the directory contents
				String files[] = file.list();

				for (String temp : files) {
					// construct the file structure
					File fileDelete = new File(file, temp);
					// recursive delete
					deleteDirectory(fileDelete);
				}
				// check the directory again, if empty then delete it
				if (file.list().length == 0) {
					file.delete();
				}
			}

		} else {
			// if file, then delete it
			file.delete();
		}
	}
}
