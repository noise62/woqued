package worst.woqued.client.features.modules.combat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import lombok.Getter;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import worst.woqued.api.system.backend.ClientInfo;
import worst.woqued.api.system.backend.SharedClass;
import worst.woqued.api.utils.rotation.RotationUtil;
import worst.woqued.api.utils.rotation.manager.Rotation;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class NeuroRotationManager {
    private static NeuroRotationManager instance;

    private boolean isRecording;
    private boolean isPlaying;
    private String currentModelName;
    
    private final List<NeuroRotationData> recordedData;
    private final List<NeuroRotationData> loadedModel;
    
    private int playbackIndex;
    private long lastPlaybackTime;
    
    private final Gson gson;
    private final File neuroDirectory;

    private NeuroRotationManager() {
        this.isRecording = false;
        this.isPlaying = false;
        this.currentModelName = null;
        this.recordedData = new ArrayList<>();
        this.loadedModel = new ArrayList<>();
        this.playbackIndex = 0;
        this.lastPlaybackTime = 0;
        
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.neuroDirectory = new File(ClientInfo.GAME_PATH, "woqued/neuro");
        
        if (!neuroDirectory.exists()) {
            neuroDirectory.mkdirs();
        }
    }

    public static NeuroRotationManager getInstance() {
        if (instance == null) {
            instance = new NeuroRotationManager();
        }
        return instance;
    }

    public void startRecording() {
        isRecording = true;
        recordedData.clear();
    }

    public void stopRecording() {
        isRecording = false;
    }

    public void recordRotation(LivingEntity target) {
        if (!isRecording || target == null || SharedClass.player() == null) return;

        Vec3d playerEyePos = SharedClass.player().getEyePos();
        Vec3d targetPos = target.getEyePos();
        
        Rotation idealRotation = RotationUtil.rotationAt(targetPos);
        Rotation currentRotation = new Rotation(SharedClass.player().getYaw(), SharedClass.player().getPitch());
        
        float deltaYaw = MathHelper.wrapDegrees(idealRotation.getYaw() - currentRotation.getYaw());
        float deltaPitch = MathHelper.clamp(idealRotation.getPitch(), -90f, 90f) - currentRotation.getPitch();
        deltaPitch = MathHelper.clamp(deltaPitch, -30f, 30f);
        
        float distance = (float) playerEyePos.distanceTo(targetPos);
        
        Vec3d targetVelocity = target.getVelocity();
        float targetSpeed = (float) Math.sqrt(targetVelocity.x * targetVelocity.x + targetVelocity.z * targetVelocity.z);
        
        NeuroRotationData data = new NeuroRotationData(
            deltaYaw,
            deltaPitch,
            distance,
            targetSpeed,
            System.currentTimeMillis()
        );
        
        recordedData.add(data);
    }

    public void saveModel(String name) {
        if (recordedData.isEmpty()) {
            return;
        }

        try {
            File modelFile = new File(neuroDirectory, name + ".json");
            String json = gson.toJson(recordedData);
            Files.write(modelFile.toPath(), json.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadModel(String name) {
        File modelFile = new File(neuroDirectory, name + ".json");
        
        if (!modelFile.exists()) {
            return;
        }

        try {
            String json = new String(Files.readAllBytes(modelFile.toPath()));
            Type listType = new TypeToken<List<NeuroRotationData>>(){}.getType();
            loadedModel.clear();
            loadedModel.addAll(gson.fromJson(json, listType));
            
            currentModelName = name;
            isPlaying = true;
            playbackIndex = 0;
            lastPlaybackTime = System.currentTimeMillis();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stopPlayback() {
        isPlaying = false;
        currentModelName = null;
        loadedModel.clear();
        playbackIndex = 0;
    }

    public Rotation getNextRotation() {
        if (!isPlaying || loadedModel.isEmpty() || SharedClass.player() == null) {
            return null;
        }

        long currentTime = System.currentTimeMillis();
        long timeSinceLastPlayback = currentTime - lastPlaybackTime;
        
        if (timeSinceLastPlayback < 50) {
            return null;
        }

        if (playbackIndex >= loadedModel.size()) {
            playbackIndex = 0;
        }

        NeuroRotationData data = loadedModel.get(playbackIndex);
        
        Rotation currentRotation = new Rotation(SharedClass.player().getYaw(), SharedClass.player().getPitch());
        float newYaw = MathHelper.wrapDegrees(currentRotation.getYaw() + data.getDeltaYaw());
        float newPitch = MathHelper.clamp(currentRotation.getPitch() + data.getDeltaPitch(), -90f, 90f);
        
        playbackIndex++;
        lastPlaybackTime = currentTime;
        
        return new Rotation(newYaw, newPitch);
    }

    public List<String> getAvailableModels() {
        if (!neuroDirectory.exists()) {
            return new ArrayList<>();
        }

        File[] files = neuroDirectory.listFiles((dir, name) -> name.endsWith(".json"));
        
        if (files == null) {
            return new ArrayList<>();
        }

        return List.of(files).stream()
            .map(file -> file.getName().replace(".json", ""))
            .collect(Collectors.toList());
    }

    public void openFolder() {
        try {
            String path = neuroDirectory.getAbsolutePath();
            ProcessBuilder pb = new ProcessBuilder("explorer.exe", path);
            pb.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}