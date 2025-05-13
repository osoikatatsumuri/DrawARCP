package com.example.drawarcp

import android.content.Context
import com.example.drawarcp.data.PermissionRepositoryImpl
import com.example.drawarcp.data.ar.ARNodeProvider
import com.example.drawarcp.data.ar.ARPlacementController
import com.example.drawarcp.domain.interfaces.IARPlacementController
import com.example.drawarcp.domain.interfaces.IPermissionsRepository
import com.example.drawarcp.domain.usecases.AddNodeUseCase
import com.example.drawarcp.domain.usecases.GetPermissionDataUseCase
import com.example.drawarcp.domain.usecases.TransformNodeUseCase
import com.google.android.filament.Engine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.sceneview.ar.arcore.ARSession
import io.github.sceneview.loaders.MaterialLoader
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ARModule {
    @Provides
    @Singleton
    fun provideARSession(@ApplicationContext context: Context): ARSession {
        return ARSession(
            context = context,
            features = setOf(),
            onResumed = { session ->
                session.resume()
            },
            onPaused = { session ->
                session.pause()
            },
            onConfigChanged = { session, config ->
                session.configure(config)
            }
        )
    }

    @Provides
    @Singleton
    fun provideARNodeProvider(): ARNodeProvider {
        return ARNodeProvider()
    }

    @Provides
    @Singleton
    fun provideMaterialLoader(@ApplicationContext context: Context, engine: Engine): MaterialLoader {
        return MaterialLoader(context = context, engine = engine)
    }

    @Provides
    @Singleton
    fun provideAddNodeUseCase(placementController: IARPlacementController) =
        AddNodeUseCase(placementController)

    @Provides
    @Singleton
    fun provideTransformNodeUseCase(provider: ARNodeProvider) =
        TransformNodeUseCase(provider)

    @Provides
    @Singleton
    fun provideGetPermissionDataUseCase(repository: IPermissionsRepository) =
        GetPermissionDataUseCase(repository)

    @Provides
    @Singleton
    fun providePermissionRepository(): IPermissionsRepository = PermissionRepositoryImpl()


    @Provides
    @Singleton
    fun provideARPlacementController(): IARPlacementController =
        ARPlacementController()
}

@Module
@InstallIn(SingletonComponent::class)
object EngineModule {
    @Provides
    @Singleton
    fun provideEngine(): Engine {
        return Engine.create()
    }
}
