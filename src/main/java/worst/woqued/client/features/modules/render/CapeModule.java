package worst.woqued.client.features.modules.render;

import lombok.Getter;
import worst.woqued.api.module.Category;
import worst.woqued.api.module.Module;
import worst.woqued.api.module.ModuleRegister;

@ModuleRegister(name = "Cape", category = Category.RENDER)
public class CapeModule extends Module {
    @Getter private static final CapeModule instance = new CapeModule();

    public CapeModule() {
        // Здесь можно добавлять настройки в будущем
    }

    @Override
    public void onEvent() {
        // Оставляем пустым.
        // Логика плаща работает напрямую в рендере (через MixinAbstractClientPlayerEntity),
        // поэтому тут обновлять ничего не нужно. Метод нужен только для компиляции.
    }
}