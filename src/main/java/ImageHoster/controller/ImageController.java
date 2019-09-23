package ImageHoster.controller;

import ImageHoster.model.Comment;
import ImageHoster.model.Image;
import ImageHoster.model.Tag;
import ImageHoster.model.User;
import ImageHoster.service.ImageService;
import ImageHoster.service.TagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

@Controller
public class ImageController {

    @Autowired
    private ImageService imageService;

    @Autowired
    private TagService tagService;

    //This method displays all the images in the user home page after successful login
    @RequestMapping("images")
    public String getUserImages(Model model) {
        List<Image> images = imageService.getAllImages();
        model.addAttribute("images", images);
        return "images";
    }


    @RequestMapping("/images/{id}/{title}")
    public String showImage(@PathVariable("id") Integer id,@PathVariable("title") String title, Model model) {
        Image image = imageService.getImageByTitle(id,title);
        model.addAttribute("image", image);
        model.addAttribute("tags", image.getTags());
        model.addAttribute("comments",image.getComments());
        return "images/image";
    }


    @RequestMapping("/images/upload")
    public String newImage() {
        return "images/upload";
    }


    @RequestMapping(value = "/images/upload", method = RequestMethod.POST)
    public String createImage(@RequestParam("file") MultipartFile file, @RequestParam("tags") String tags, Image newImage, HttpSession session) throws IOException {

        User user = (User) session.getAttribute("loggeduser");
        newImage.setUser(user);
        String uploadedImageData = convertUploadedFileToBase64(file);
        newImage.setImageFile(uploadedImageData);

        List<Tag> imageTags = findOrCreateTags(tags);
        newImage.setTags(imageTags);
        newImage.setDate(new Date());
        imageService.uploadImage(newImage);
        return "redirect:/images";
    }


    @RequestMapping(value = "/editImage")
    public String editImage(@RequestParam("imageId") Integer imageId, Model model, HttpSession session) {
        User user = (User) session.getAttribute("loggeduser");

        //check whether the user trying to edit is the owner of the image or not
        Image image = imageService.getImage(imageId);

        if(this.imageService.confirmOwner(imageId, user.getUsername())){
            // editor is the owner
            String tags = convertTagsToString(image.getTags());

            model.addAttribute("image", image);
            model.addAttribute("tags", tags);
            return "images/edit";
        }

        else{
            //not the owner
            String error = "Only the owner of the image can edit the image";
            model.addAttribute("editError", error);
            model.addAttribute("image", image);
            model.addAttribute("comments",image.getComments());
            return "images/image";
        }

    }

    @RequestMapping(value = "/editImage", method = RequestMethod.PUT)
    public String editImageSubmit(@RequestParam("file") MultipartFile file, @RequestParam("imageId") Integer imageId,
                                  @RequestParam("tags") String tags,
                                  Image updatedImage,
                                  HttpSession session) throws IOException {

        Image image = imageService.getImage(imageId);
        String updatedImageData = convertUploadedFileToBase64(file);
        List<Tag> imageTags = findOrCreateTags(tags);

        if (updatedImageData.isEmpty())
            updatedImage.setImageFile(image.getImageFile());
        else {
            updatedImage.setImageFile(updatedImageData);
        }

        updatedImage.setId(imageId);
        User user = (User) session.getAttribute("loggeduser");
        updatedImage.setUser(user);
        updatedImage.setTags(imageTags);
        updatedImage.setDate(new Date());

        imageService.updateImage(updatedImage);
        return "redirect:/images/" + updatedImage.getTitle();
    }


    @RequestMapping(value = "/deleteImage", method = RequestMethod.DELETE)
    public String deleteImageSubmit(@RequestParam(name = "imageId") Integer imageId, HttpSession session, Model model) {

        User user = (User) session.getAttribute("loggeduser");
        Image image = imageService.getImage(imageId);

        //check whether the user trying to edit is the owner of the image or not
        if(this.imageService.confirmOwner(imageId, user.getUsername())){
            //yes delete the image
            this.imageService.deleteImage(imageId);
            return "redirect:/images";
        }else{
            String error = "Only the owner of the image can delete the image";
            model.addAttribute("deleteError", error);
            model.addAttribute("image", image);
            model.addAttribute("comments",image.getComments());
            return "images/image";
        }
    }

    @RequestMapping(value = "/image/{imageId}/{imageTitle}/comments", method = RequestMethod.POST)
    public String createComment(@PathVariable("imageId") Integer imageId,
                              @PathVariable("imageTitle") String imageTitle,
                                @RequestParam("comment") String comment,
                              HttpSession session,
                              Comment newComment,
                              Image updatedImage, Model model){

        User user = (User) session.getAttribute("loggeduser");
        newComment.setUser(user);

        Image image = this.imageService.getImage(imageId);
        newComment.setCreatedDate(LocalDate.now());
        newComment.setId(imageId);
        newComment.setImage(image);
        newComment.setText(comment);

        //create a new comment in the database*/

        this.imageService.createComment(newComment);

        return this.showImage(imageId,imageTitle,model);

    }


    //This method converts the image to Base64 format
    private String convertUploadedFileToBase64(MultipartFile file) throws IOException {
        return Base64.getEncoder().encodeToString(file.getBytes());
    }


    private List<Tag> findOrCreateTags(String tagNames) {
        StringTokenizer st = new StringTokenizer(tagNames, ",");
        List<Tag> tags = new ArrayList<Tag>();

        while (st.hasMoreTokens()) {
            String tagName = st.nextToken().trim();
            Tag tag = tagService.getTagByName(tagName);

            if (tag == null) {
                Tag newTag = new Tag(tagName);
                tag = tagService.createTag(newTag);
            }
            tags.add(tag);
        }
        return tags;
    }


    //Converts the list of all tags to a single string containing all the tags separated by a comma
    private String convertTagsToString(List<Tag> tags) {
        StringBuilder tagString = new StringBuilder();

        for (int i = 0; i <= tags.size() - 2; i++) {
            tagString.append(tags.get(i).getName()).append(",");
        }

        Tag lastTag = tags.get(tags.size() - 1);
        tagString.append(lastTag.getName());

        return tagString.toString();
    }
}
