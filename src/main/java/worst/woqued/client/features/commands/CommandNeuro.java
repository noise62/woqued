package worst.woqued.client.features.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import worst.woqued.api.command.Command;
import worst.woqued.api.command.CommandRegister;
import worst.woqued.client.features.commands.arguments.StrictlyNeuroNameArgument;
import worst.woqued.client.features.modules.combat.AuraModule;
import worst.woqued.client.features.modules.combat.NeuroRotationManager;

import java.io.File;

@CommandRegister(name = "neuro")
public class CommandNeuro extends Command {
    @Override
    public void execute(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("record").executes(context -> {
            NeuroRotationManager manager = NeuroRotationManager.getInstance();
            
            if (manager.isRecording()) {
                print("Запись уже активна.");
                return SINGLE_SUCCESS;
            }
            
            if (manager.isPlaying()) {
                manager.stopPlayback();
                print("Воспроизведение остановлено.");
            }
            
            manager.startRecording();
            print("Запись данных начата. Ротация отключена, TriggerBot активен.");
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("stop").executes(context -> {
            NeuroRotationManager manager = NeuroRotationManager.getInstance();
            
            if (!manager.isRecording() && !manager.isPlaying()) {
                print("Нет активной записи или воспроизведения.");
                return SINGLE_SUCCESS;
            }
            
            if (manager.isRecording()) {
                manager.stopRecording();
                print("Запись остановлена. Ротация отключена, TriggerBot активен.");
            }
            
            if (manager.isPlaying()) {
                manager.stopPlayback();
                print("Воспроизведение остановлено.");
            }
            
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("save")
                .then(argument("name", StringArgumentType.string())
                        .executes(context -> {
                            String name = StringArgumentType.getString(context, "name");
                            NeuroRotationManager manager = NeuroRotationManager.getInstance();
                            
                            if (manager.getRecordedData().isEmpty()) {
                                print("Нет данных для сохранения. Сначала запишите данные с помощью 'neuro record'.");
                                return SINGLE_SUCCESS;
                            }
                            
                            manager.saveModel(name);
                            print("Модель '" + name + "' сохранена успешно.");
                            return SINGLE_SUCCESS;
                        })
                )
        );

        builder.then(literal("play")
                .then(argument("name", StrictlyNeuroNameArgument.create())
                        .executes(context -> {
                            String name = StringArgumentType.getString(context, "name");
                            NeuroRotationManager manager = NeuroRotationManager.getInstance();
                            
                            File modelFile = new File(manager.getNeuroDirectory(), name + ".json");
                            if (!modelFile.exists()) {
                                print("Модель '" + name + "' не найдена. Используйте 'neuro list' для просмотра доступных моделей.");
                                return SINGLE_SUCCESS;
                            }
                            
                            if (manager.isRecording()) {
                                manager.stopRecording();
                                print("Запись остановлена.");
                            }
                            
                            manager.loadModel(name);
                            print("Модель '" + name + "' загружена. Neuro Rotation активирован.");
                            return SINGLE_SUCCESS;
                        })
                )
        );

        builder.then(literal("list").executes(context -> {
            NeuroRotationManager manager = NeuroRotationManager.getInstance();
            var models = manager.getAvailableModels();
            
            if (models.isEmpty()) {
                print("Список моделей пуст. Используйте 'neuro record' для записи данных.");
            } else {
                print("Доступные модели: " + String.join(", ", models));
            }
            
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("folder").executes(context -> {
            NeuroRotationManager manager = NeuroRotationManager.getInstance();
            manager.openFolder();
            print("Открываю папку с моделями...");
            return SINGLE_SUCCESS;
        }));
    }
}